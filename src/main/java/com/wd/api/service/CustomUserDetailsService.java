package com.wd.api.service;

import com.wd.api.repository.PortalUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Portal user details service - loads ONLY portal users (company employees).
 * 
 * Customer users authenticate exclusively via wd_customer_api.
 * There is no CUSTOMER role in the portal - this is by design for security.
 * The portal is strictly for company employees (ADMIN, USER, PROJECT_MANAGER, etc.).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Only load portal users (company employees) - no customer authentication
        return portalUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Portal login attempt failed - user not found: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
    }
}
