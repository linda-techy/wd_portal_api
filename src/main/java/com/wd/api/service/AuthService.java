package com.wd.api.service;

import com.wd.api.dto.LoginRequest;
import com.wd.api.dto.LoginResponse;
import com.wd.api.dto.RefreshTokenRequest;
import com.wd.api.dto.RefreshTokenResponse;
import com.wd.api.model.RefreshToken;
import com.wd.api.model.User;
import com.wd.api.repository.RefreshTokenRepository;
import com.wd.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PermissionService permissionService;

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));

        // Reuse authenticated principal to avoid extra DB hit
        User user = (User) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Save refresh token
        saveRefreshToken(user, refreshToken);

        // Get user permissions (ADMIN users will get ALL permissions automatically)
        List<String> permissions = permissionService.getUserPermissions(user);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName(),
                user.getRole().getCode());

        return new LoginResponse(accessToken, refreshToken, jwtService.getAccessTokenExpiration(), userInfo,
                permissions);
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        System.out.println("Processing refresh token request. Token length: "
                + (refreshToken != null ? refreshToken.length() : "null"));

        // Validate refresh token
        if (!jwtService.validateToken(refreshToken)) {
            System.out.println("Refresh token validation failed: Signature or format invalid");
            throw new RuntimeException("Invalid refresh token");
        }

        // Get user from refresh token
        String userEmail = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    System.out.println("User not found for email: " + userEmail);
                    return new RuntimeException("User not found");
                });

        // Check if refresh token exists and is not revoked
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    System.out.println("Refresh token not found in database for user: " + userEmail);
                    return new RuntimeException("Refresh token not found");
                });

        if (storedToken.isExpired()) {
            System.out.println("Refresh token expired for user: " + userEmail);
            throw new RuntimeException("Refresh token expired");
        }

        if (storedToken.getRevoked()) {
            System.out.println("Refresh token revoked for user: " + userEmail);
            throw new RuntimeException("Refresh token revoked");
        }

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user);
        System.out.println("Successfully refreshed token for user: " + userEmail);

        return new RefreshTokenResponse(newAccessToken, jwtService.getAccessTokenExpiration());
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    public LoginResponse.UserInfo getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName(),
                user.getRole().getCode());
    }

    private void saveRefreshToken(User user, String refreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(refreshToken);
        token.setExpiryDate(LocalDateTime.now().plusDays(7)); // 7 days
        token.setRevoked(false);

        refreshTokenRepository.save(token);
    }
}