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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenStr = request.getRefreshToken();

        // 1. Basic format validation
        if (!jwtService.validateToken(refreshTokenStr)) {
            throw new RuntimeException("Invalid refresh token format");
        }

        // 2. Retrieve token from DB
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        User user = storedToken.getUser();

        // 3. Replay Protection: If token is revoked, it might be a reuse attack
        if (storedToken.getRevoked()) {
            // Revoke all tokens for this user for safety
            refreshTokenRepository.deleteByUser_Id(user.getId());
            throw new RuntimeException(
                    "Refresh token has been revoked - possible replay attack detected. All sessions invalidated.");
        }

        // 4. Check Expiry
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("Refresh token expired");
        }

        // 5. Rotate Token: Revoke current and issue new
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenStr = jwtService.generateRefreshToken(user);

        // Save new refresh token
        saveRefreshToken(user, newRefreshTokenStr);

        return new RefreshTokenResponse(newAccessToken, newRefreshTokenStr, jwtService.getAccessTokenExpiration());
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