package com.wd.api.controller;

import com.wd.api.dto.LoginRequest;
import com.wd.api.dto.LoginResponse;
import com.wd.api.dto.RefreshTokenRequest;
import com.wd.api.dto.RefreshTokenResponse;
import com.wd.api.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Login validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Login failed for user {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid email or password"));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            RefreshTokenResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Refresh token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Refresh token failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid or expired refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            authService.logout(request.getRefreshToken());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Logout failed: {}", e.getMessage(), e);
            // Still return 200 for logout - client should clear tokens regardless
            return ResponseEntity.ok().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        try {
            LoginResponse.UserInfo userInfo = authService.getCurrentUser(email);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            logger.error("Failed to get current user for email {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Authentication failed"));
        }
    }

    /**
     * Register or update the FCM device token for the authenticated portal user.
     * Called by the portal Flutter app immediately after login.
     * POST /auth/fcm-token   body: { "fcmToken": "..." }
     */
    @PostMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> registerFcmToken(@RequestBody Map<String, String> body,
                                               Authentication authentication) {
        try {
            String fcmToken = body.get("fcmToken");
            if (fcmToken == null || fcmToken.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "fcmToken is required"));
            }
            authService.registerFcmToken(authentication.getName(), fcmToken);
            return ResponseEntity.ok(Map.of("message", "FCM token registered"));
        } catch (Exception e) {
            logger.error("Failed to register FCM token for {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to register FCM token"));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth controller is working!");
    }

}
