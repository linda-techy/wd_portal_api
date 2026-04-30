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
    @Value("${app.admin.seed-password:Test123$}")
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
        portalUserRepository.findByEmail(adminEmail).ifPresentOrElse(
                admin -> {
                    // Sync password with seed-password in local dev
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    portalUserRepository.save(admin);
                    logger.info("Admin user '{}' password synchronized with seed value", adminEmail);
                },
                () -> {
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

                                logger.warn("================================================================");
                                logger.warn("  INITIAL ADMIN USER CREATED");
                                logger.warn("  Email    : {}", adminEmail);
                                logger.warn("  Password : {}", adminPassword);
                                logger.warn("================================================================");
                            },
                            () -> logger.error("ADMIN role not found in portal_roles table.")
                    );
                }
        );
    }
}
