package com.wd.api.service;

import com.wd.api.dto.LoginRequest;
import com.wd.api.dto.LoginResponse;
import com.wd.api.dto.RefreshTokenRequest;
import com.wd.api.dto.RefreshTokenResponse;
import com.wd.api.model.RefreshToken;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.RefreshTokenRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.util.TokenHashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PortalUserRepository portalUserRepository;

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
        PortalUser user = (PortalUser) authentication.getPrincipal();

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

        // 2. Retrieve token from DB (stored as SHA-256 hash)
        String tokenHash = TokenHashUtil.hash(refreshTokenStr);
        RefreshToken storedToken = refreshTokenRepository.findByToken(tokenHash)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        PortalUser user = storedToken.getUser();

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
        // Hash before lookup — tokens are stored as SHA-256 hashes
        refreshTokenRepository.deleteByToken(TokenHashUtil.hash(refreshToken));
    }

    /**
     * Store or update the FCM device token for a portal user.
     * One active token per user (latest device wins).
     */
    @org.springframework.transaction.annotation.Transactional
    public void registerFcmToken(String email, String fcmToken) {
        PortalUser user = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        user.setFcmToken(fcmToken);
        portalUserRepository.save(user);
    }

    public LoginResponse.UserInfo getCurrentUser(String email) {
        PortalUser user = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName(),
                user.getRole().getCode());
    }

    /**
     * Get current authenticated user from Security Context
     * This overloaded method retrieves the email from Spring Security's
     * Authentication
     */
    public PortalUser getCurrentUser() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        String email = authentication.getName();

        return portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private void saveRefreshToken(PortalUser user, String rawRefreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        // Store only the SHA-256 hash — the raw JWT is never persisted to DB
        token.setToken(TokenHashUtil.hash(rawRefreshToken));
        token.setExpiryDate(LocalDateTime.now().plusDays(7)); // 7 days
        token.setRevoked(false);

        refreshTokenRepository.save(token);
    }

    /**
     * Nightly cleanup: purge all expired or revoked refresh tokens.
     * Without this, the refresh_tokens table grows unbounded — in a 100k user app it
     * will contain millions of rows within weeks, making findByToken() a full table scan.
     * Runs at 2:00 AM IST daily. Bulk deletion with @Modifying avoids OOM vs. deleteAll(List).
     */
    @Transactional
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Kolkata")
    public void cleanupExpiredRefreshTokens() {
        try {
            int deleted = refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
            logger.info("Nightly refresh token cleanup: deleted {} expired/revoked entries", deleted);
        } catch (Exception e) {
            logger.error("Error during nightly refresh token cleanup", e);
        }
    }

}