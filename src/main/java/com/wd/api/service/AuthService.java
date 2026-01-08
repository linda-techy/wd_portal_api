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

    /**
     * Get current authenticated user from Security Context
     * This overloaded method retrieves the email from Spring Security's
     * Authentication
     */
    public com.wd.api.model.PortalUser getCurrentUser() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        String email = authentication.getName();

        // Try to find portal user by email
        java.util.Optional<com.wd.api.model.PortalUser> portalUserOpt = portalUserRepository.findByEmail(email);

        if (portalUserOpt.isPresent()) {
            return portalUserOpt.get();
        }

        // Fallback: Return a minimal PortalUser based on User entity
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        com.wd.api.model.PortalUser portalUser = new com.wd.api.model.PortalUser();
        portalUser.setId(user.getId());
        portalUser.setEmail(user.getEmail());
        portalUser.setFirstName(user.getFirstName());
        portalUser.setLastName(user.getLastName());
        portalUser.setEnabled(user.getEnabled());

        return portalUser;
    }

    @Autowired
    private com.wd.api.repository.PortalUserRepository portalUserRepository;

    private void saveRefreshToken(User user, String refreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(refreshToken);
        token.setExpiryDate(LocalDateTime.now().plusDays(7)); // 7 days
        token.setRevoked(false);

        refreshTokenRepository.save(token);
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.wd.api.repository.RoleRepository roleRepository;

    @Transactional
    public void createTestUser() {
        if (userRepository.findByEmail("admin@test.com").isPresent()) {
            return;
        }

        com.wd.api.model.Role adminRole = roleRepository.findAll().stream()
                .filter(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "ADMIN".equalsIgnoreCase(r.getCode()))
                .findFirst()
                .orElse(roleRepository.findById(1L).orElse(roleRepository.findById(5L).orElse(null)));

        if (adminRole == null) {
            // Fallback: This might fail if sequence is out of sync, but we have no choice
            // if table is empty
            try {
                com.wd.api.model.Role role = new com.wd.api.model.Role();
                role.setName("ADMIN");
                role.setCode("ADMIN");
                role.setDescription("Administrator Role");
                adminRole = roleRepository.save(role);
            } catch (Exception e) {
                // Ignore if duplicate
                adminRole = roleRepository.findByName("ADMIN").orElse(null);
            }
        }

        if (adminRole == null) {
            throw new RuntimeException("Could not find or create a Role for test user.");
        }

        User user = new User();
        user.setEmail("admin@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setFirstName("Test");
        user.setLastName("Admin");
        user.setRole(adminRole);
        user.setEnabled(true);

        userRepository.save(user);
    }
}