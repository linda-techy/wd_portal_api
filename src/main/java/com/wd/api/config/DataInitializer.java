package com.wd.api.config;

import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalRoleRepository;
import com.wd.api.repository.PortalUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once on application startup.
 *
 * <p>Seeds an initial ADMIN portal user when the {@code portal_users} table contains
 * no account with the configured admin email. This covers:
 * <ul>
 *   <li>Local dev — Flyway is disabled, so no migration can seed the row.</li>
 *   <li>Production / Staging — first boot after a clean schema deploy.</li>
 * </ul>
 *
 * <p>The seeding is fully idempotent: it checks by email before inserting,
 * so restarting the application never creates duplicate rows.
 *
 * <p>The default password is intentionally logged at WARN level so it appears
 * in every environment's log output on first boot. Change it immediately
 * via the portal UI or the /auth/forgot-password flow.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    /** Default admin email — can be overridden via {@code app.admin.seed-email}. */
    @Value("${app.admin.seed-email:admin@walldotbuilders.com}")
    private String adminEmail;

    /** Default admin password — can be overridden via {@code app.admin.seed-password}. */
    @Value("${app.admin.seed-password:Admin@2025}")
    private String adminPassword;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private PortalRoleRepository portalRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUser();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void seedAdminUser() {
        // Already exists — nothing to do
        if (portalUserRepository.findByEmail(adminEmail).isPresent()) {
            logger.debug("Admin user '{}' already exists — skipping seed", adminEmail);
            return;
        }

        portalRoleRepository.findByCode("ADMIN").ifPresentOrElse(
                adminRole -> {
                    PortalUser admin = new PortalUser();
                    admin.setEmail(adminEmail);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setFirstName("System");
                    admin.setLastName("Admin");
                    admin.setRole(adminRole);
                    admin.setEnabled(true);

                    portalUserRepository.save(admin);

                    // WARN level so this is visible in every env's log even with INFO filtering
                    logger.warn("================================================================");
                    logger.warn("  INITIAL ADMIN USER CREATED — CHANGE PASSWORD IMMEDIATELY");
                    logger.warn("  Email    : {}", adminEmail);
                    logger.warn("  Password : {}", adminPassword);
                    logger.warn("  Use the portal UI or /auth/forgot-password to reset it.");
                    logger.warn("================================================================");
                },
                () -> logger.error(
                        "ADMIN role not found in portal_roles table. " +
                        "The database migrations (V5) must run before the app can seed an admin user. " +
                        "Start the app with a production/staging profile to trigger Flyway, " +
                        "or insert the ADMIN role manually.")
        );
    }
}
