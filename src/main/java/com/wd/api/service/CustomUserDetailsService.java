package com.wd.api.service;

import com.wd.api.repository.PortalUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private com.wd.api.repository.CustomerUserRepository customerUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // First try to find a portal user (admin, staff, etc.)
        java.util.Optional<com.wd.api.model.PortalUser> portalUser = portalUserRepository.findByEmail(email);
        if (portalUser.isPresent()) {
            return portalUser.get();
        }

        // If not found, try to find a customer user
        com.wd.api.model.CustomerUser customerUser = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Map CustomerUser to PortalUser (which implements UserDetails)
        // This allows customers to authenticate using the same token mechanism
        com.wd.api.model.PortalUser user = new com.wd.api.model.PortalUser();
        user.setId(customerUser.getId());
        user.setEmail(customerUser.getEmail());
        user.setPassword(customerUser.getPassword());
        user.setFirstName(customerUser.getFirstName());
        user.setLastName(customerUser.getLastName());
        user.setEnabled(customerUser.getEnabled());

        // Create a transient PortalRole for the customer
        com.wd.api.model.PortalRole role = new com.wd.api.model.PortalRole();
        if (customerUser.getRole() != null) {
            role.setId(customerUser.getRole().getId());
            role.setName(customerUser.getRole().getName());
            role.setCode("CUSTOMER"); // Ensure code matches ROLE_CUSTOMER expectation
            role.setDescription(customerUser.getRole().getDescription());
        } else {
            role.setId(0L);
            role.setName("CUSTOMER");
            role.setCode("CUSTOMER");
            role.setDescription("Customer Role");
        }
        user.setRole(role);

        return user;
    }
}