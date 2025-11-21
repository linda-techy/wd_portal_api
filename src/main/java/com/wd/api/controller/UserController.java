package com.wd.api.controller;

import com.wd.api.dto.TeamMemberDTO;
import com.wd.api.model.User;
import com.wd.api.repository.UserRepository;
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
    private UserRepository userRepository;
    
    /**
     * Get all team members (users with role_id = 8)
     * Returns simplified DTO with id, first_name, last_name, full_name, and email
     */
    @GetMapping("/team-members")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<TeamMemberDTO>> getTeamMembers() {
        try {
            List<User> users = userRepository.findByRoleId(8L);
            
            List<TeamMemberDTO> teamMembers = users.stream()
                    .map(user -> new TeamMemberDTO(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(teamMembers);
        } catch (Exception e) {
            logger.error("Error fetching team members", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

