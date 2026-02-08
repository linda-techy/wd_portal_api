package com.wd.api.config;

import com.wd.api.model.PortalUser;
import com.wd.api.model.CustomerUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditConfig {

    @Bean
    @SuppressWarnings("null")
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            SecurityContext context = SecurityContextHolder.getContext();
            if (context == null)
                return Optional.empty();

            Authentication authentication = context.getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof PortalUser) {
                return Optional.of(((PortalUser) principal).getId());
            } else if (principal instanceof CustomerUser) {
                return Optional.of(((CustomerUser) principal).getId());
            }

            return Optional.empty();
        };
    }
}
