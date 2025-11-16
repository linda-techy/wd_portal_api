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
import java.util.stream.Collectors;

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
    
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword()
            )
        );
        
        // Reuse authenticated principal to avoid extra DB hit
        User user = (User) authentication.getPrincipal();
        
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // Save refresh token
        saveRefreshToken(user, refreshToken);
        
        // Get user permissions
        List<String> permissions = user.getAuthorities().stream()
            .map(Object::toString)
            .collect(Collectors.toList());
        
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole().getName()
        );
        
        return new LoginResponse(accessToken, refreshToken, jwtService.getAccessTokenExpiration(), userInfo, permissions);
    }
    
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        // Validate refresh token
        if (!jwtService.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        // Get user from refresh token
        String userEmail = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if refresh token exists and is not revoked
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        
        if (storedToken.isExpired() || storedToken.getRevoked()) {
            throw new RuntimeException("Refresh token expired or revoked");
        }
        
        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user);
        
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
            user.getRole().getName()
        );
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