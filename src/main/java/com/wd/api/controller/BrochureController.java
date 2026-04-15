package com.wd.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Public endpoint for serving the company brochure PDF.
 * No authentication required — brochure is a public marketing asset.
 *
 * File location (environment-driven via storageBasePath):
 *   local:      N:\Projects\wd projects git\storage\brochure\Walldot-Builders-Brochure.pdf
 *   production: /home/ftpuser/var/www/app/walldotbuilders/storage/brochure/Walldot-Builders-Brochure.pdf
 */
@RestController
@RequestMapping("/api/brochure")
public class BrochureController {

    private static final Logger logger = LoggerFactory.getLogger(BrochureController.class);
    private static final String BROCHURE_FILENAME = "Walldot-Builders-Brochure.pdf";

    @Value("${storageBasePath}")
    private String storageBasePath;

    @GetMapping("/download")
    public ResponseEntity<?> downloadBrochure() {
        try {
            Path brochurePath = Paths.get(storageBasePath, "brochure", BROCHURE_FILENAME).normalize();

            // Prevent path traversal
            Path baseDir = Paths.get(storageBasePath, "brochure").normalize();
            if (!brochurePath.startsWith(baseDir)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!Files.exists(brochurePath)) {
                logger.warn("Brochure file not found at: {}", brochurePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Brochure is not available at this time. Please contact us at info@walldotbuilders.com\"}");
            }

            Resource resource = new UrlResource(brochurePath.toUri());
            if (!resource.isReadable()) {
                logger.error("Brochure file exists but is not readable: {}", brochurePath);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\": \"Unable to read brochure file.\"}");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + BROCHURE_FILENAME + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving brochure: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"An error occurred. Please try again later.\"}");
        }
    }
}
