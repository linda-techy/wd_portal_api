package com.wd.api.controller;

import com.wd.api.dto.PortalUserCreateRequest;
import com.wd.api.dto.PortalUserResponse;
import com.wd.api.dto.PortalUserUpdateRequest;
import com.wd.api.dto.PortalRoleDTO;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.PortalRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/portal-users")
public class PortalUserController {

    private static final Logger logger = LoggerFactory.getLogger(PortalUserController.class);

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private PortalRoleRepository portalRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Get all portal users
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    // verifying role names in database
    public ResponseEntity<?> getAllPortalUsers() {
        try {
            List<PortalUser> users = portalUserRepository.findAll();
            List<PortalUserResponse> responses = users.stream()
                    .map(PortalUserResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching portal users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get paginated portal users
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> getPortalUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search) {
        try {
            // Validate parameters
            if (page < 0) {
                return ResponseEntity.badRequest().body("Page must be >= 0");
            }
            if (size < 1 || size > 100) {
                return ResponseEntity.badRequest().body("Size must be between 1 and 100");
            }

            // Create pageable
            org.springframework.data.domain.Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                    ? org.springframework.data.domain.Sort.Direction.DESC
                    : org.springframework.data.domain.Sort.Direction.ASC;

            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,
                    size, sortDirection, sort);

            // Get paginated results with optional search
            org.springframework.data.domain.Page<PortalUser> userPage;
            if (search != null && !search.trim().isEmpty()) {
                userPage = portalUserRepository.searchUsers(search.trim(), pageable);
            } else {
                userPage = portalUserRepository.findAll(pageable);
            }

            // Convert to response DTOs
            org.springframework.data.domain.Page<PortalUserResponse> responsePage = userPage
                    .map(PortalUserResponse::new);

            return ResponseEntity.ok(responsePage);
        } catch (Exception e) {
            logger.error("Error fetching paginated portal users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get portal user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPortalUserById(@PathVariable Long id) {
        try {
            Optional<PortalUser> userOpt = portalUserRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(new PortalUserResponse(userOpt.get()));
        } catch (Exception e) {
            logger.error("Error fetching portal user with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create portal user
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    // names in database
    public ResponseEntity<?> createPortalUser(@Valid @RequestBody PortalUserCreateRequest request) {
        try {
            // Validate required fields
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }

            // Check if email already exists
            if (portalUserRepository.findByEmail(request.getEmail().trim()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already exists");
            }

            // Validate password
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }

            if (request.getPassword().trim().length() < 6) {
                return ResponseEntity.badRequest().body("Password must be at least 6 characters");
            }

            // Create new user
            PortalUser user = new PortalUser();
            user.setEmail(request.getEmail().trim().toLowerCase());
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
            user.setFirstName(request.getFirstName().trim());
            user.setLastName(request.getLastName().trim());

            // Set role by looking up PortalRole entity
            if (request.getRoleId() != null) {
                portalRoleRepository.findById(request.getRoleId()).ifPresent(user::setRole);
            }

            user.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

            PortalUser savedUser = portalUserRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(new PortalUserResponse(savedUser));

        } catch (Exception e) {
            logger.error("Error creating portal user", e);
            return ResponseEntity.internalServerError().body("Error creating portal user");
        }
    }

    /**
     * Update portal user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    // verifying role names in database
    public ResponseEntity<?> updatePortalUser(@PathVariable Long id,
            @Valid @RequestBody PortalUserUpdateRequest request) {
        try {
            // Validate ID
            if (id == null) {
                return ResponseEntity.badRequest().body("User ID is required");
            }

            Optional<PortalUser> userOpt = portalUserRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            PortalUser user = userOpt.get();

            // Validate request
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }

            // Update email if provided and check uniqueness
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                String newEmail = request.getEmail().trim().toLowerCase();
                if (!newEmail.equals(user.getEmail())) {
                    Optional<PortalUser> existingUser = portalUserRepository.findByEmail(newEmail);
                    if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                        return ResponseEntity.badRequest().body("Email already exists");
                    }
                    user.setEmail(newEmail);
                }
            }

            // Update password if provided
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                if (request.getPassword().trim().length() < 6) {
                    return ResponseEntity.badRequest().body("Password must be at least 6 characters");
                }
                user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
            }

            // Update other fields
            if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
                user.setFirstName(request.getFirstName().trim());
            }

            if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
                user.setLastName(request.getLastName().trim());
            }

            if (request.getRoleId() != null) {
                portalRoleRepository.findById(request.getRoleId()).ifPresent(user::setRole);
            }

            if (request.getEnabled() != null) {
                user.setEnabled(request.getEnabled());
            }

            PortalUser updatedUser = portalUserRepository.save(user);
            return ResponseEntity.ok(new PortalUserResponse(updatedUser));

        } catch (Exception e) {
            logger.error("Error updating portal user with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error updating portal user");
        }
    }

    /**
     * Delete portal user
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    // names in database
    public ResponseEntity<?> deletePortalUser(@PathVariable Long id) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body("User ID is required");
            }

            if (!portalUserRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            portalUserRepository.deleteById(id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error deleting portal user with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all portal roles for dropdown
     */
    @GetMapping("/roles")
    public ResponseEntity<?> getPortalRoles() {
        try {
            List<PortalRoleDTO> roles = portalRoleRepository.findAll().stream()
                    .map(PortalRoleDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Error fetching portal roles", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Change password
     */
    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    // verifying role names
    public ResponseEntity<?> changePassword(@PathVariable Long id,
            @Valid @RequestBody com.wd.api.dto.ChangePasswordRequest request) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body("User ID is required");
            }

            Optional<PortalUser> userOpt = portalUserRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            PortalUser user = userOpt.get();

            // Validate current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body("Incorrect current password");
            }

            // Validate new password
            if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
                return ResponseEntity.badRequest().body("New password must be at least 6 characters");
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
            portalUserRepository.save(user);

            return ResponseEntity.ok().body("Password changed successfully");

        } catch (Exception e) {
            logger.error("Error changing password for user ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error changing password");
        }
    }
}
