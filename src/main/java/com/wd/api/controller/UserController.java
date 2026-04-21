package com.wd.api.controller;

import com.wd.api.dto.TeamMemberDTO;
import com.wd.api.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private com.wd.api.repository.PortalUserRepository portalUserRepository;

    @Autowired
    private com.wd.api.repository.CustomerUserRepository customerUserRepository;

    /**
     * Get all team members (Portal Users and Customer Users)
     * Returns simplified DTO with id, first_name, last_name, full_name, email, and
     * type
     */
    @GetMapping("/team-members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TeamMemberDTO>>> getTeamMembers() {
        try {
            List<TeamMemberDTO> teamMembers = new java.util.ArrayList<>();

            // Fetch Portal Users — include role + enabled so the team-members
            // screen can show Active/Inactive and role-based department.
            List<com.wd.api.model.PortalUser> portalUsers = portalUserRepository.findAll();
            teamMembers.addAll(portalUsers.stream()
                    .map(user -> {
                        com.wd.api.model.PortalRole role = user.getRole();
                        Long roleId = role != null ? role.getId() : null;
                        String roleName = role != null ? role.getName() : null;
                        return new TeamMemberDTO(
                                user.getId(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getEmail(),
                                "PORTAL",
                                Boolean.TRUE.equals(user.getEnabled()),
                                roleId,
                                roleName,
                                roleName,   // designation mirrors role
                                roleName);  // department mirrors role
                    })
                    .collect(Collectors.toList()));

            // Fetch Customer Users
            List<com.wd.api.model.CustomerUser> customerUsers = customerUserRepository.findAll();
            teamMembers.addAll(customerUsers.stream()
                    .map(user -> new TeamMemberDTO(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            "CUSTOMER",
                            Boolean.TRUE.equals(user.getEnabled()),
                            null,
                            "CUSTOMER",
                            "Customer",
                            "Customer"))
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(ApiResponse.success("Team members retrieved successfully", teamMembers));
        } catch (Exception e) {
            logger.error("Error fetching team members", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching team members"));
        }
    }
}
