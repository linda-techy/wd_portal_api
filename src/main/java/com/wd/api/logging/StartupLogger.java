package com.wd.api.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Logs startup diagnostics for the Portal API and creates log directory.
 */
@Component
@Order(1)
public class StartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);
    private static final String LOG_DIR = "/home/backenduser/logs/portal-api";

    private final Environment environment;

    @Value("${spring.datasource.url:NOT_CONFIGURED}")
    private String dbUrl;

    @Value("${server.port:8081}")
    private String serverPort;

    public StartupLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureLogDir();
        logDiagnostics();
    }

    private void ensureLogDir() {
        try {
            var path = Paths.get(LOG_DIR);
            if (!Files.exists(path)) Files.createDirectories(path);
            Files.createDirectories(Paths.get(LOG_DIR, "archived"));
        } catch (Exception e) {
            log.warn("{} | Could not create log dir '{}': {}", LoggingConstants.PREFIX_STARTUP, LOG_DIR, e.getMessage());
        }
    }

    private void logDiagnostics() {
        String[] profiles = environment.getActiveProfiles();
        String profile = profiles.length > 0 ? String.join(",", profiles) : "default";

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║          WD PORTAL API — STARTUP DIAGNOSTICS             ║");
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║  Java Version  : {}", System.getProperty("java.version"));
        log.info("║  Spring Profile: {}", profile);
        log.info("║  Server Port   : {}", serverPort);
        log.info("║  Database URL  : {}", dbUrl.replaceAll("(?i)(password=)[^&;]+", "$1****"));
        log.info("║  Log Directory : {}", LOG_DIR);
        log.info("╚══════════════════════════════════════════════════════════╝");
    }
}
