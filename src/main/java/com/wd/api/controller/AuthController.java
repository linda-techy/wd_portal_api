package com.wd.api.controller;

import com.wd.api.dto.LoginRequest;
import com.wd.api.dto.LoginResponse;
import com.wd.api.dto.RefreshTokenRequest;
import com.wd.api.dto.RefreshTokenResponse;
import com.wd.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<LoginResponse.UserInfo> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        LoginResponse.UserInfo userInfo = authService.getCurrentUser(email);
        return ResponseEntity.ok(userInfo);
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth controller is working!");
    }
    
    @PostMapping("/create-test-user")
    public ResponseEntity<String> createTestUser() {
        try {
            // This is a temporary endpoint for testing
            // In production, you should use proper user management
            return ResponseEntity.ok("Use your database to create users or implement proper user management");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
} 