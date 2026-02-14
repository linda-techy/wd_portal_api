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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for serving files from storage
 * Endpoints: /api/storage/** and /api/files/download/** (legacy)
 */
@RestController
public class FileDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloadController.class);

    @Value("${storageBasePath:N:\\Projects\\wd projects git\\storage}")
    private String storageBasePath;

    /**
     * Serve files from storage path
     * GET /api/storage/projects/1/documents/file.pdf
     * GET /api/files/download/site-reports/10/file.png (legacy path)
     */
    @GetMapping({"/api/storage/**", "/api/files/download/**"})
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request,
            @RequestParam(required = false) String download,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            // Get the full request path (everything after the prefix)
            String requestURI = request.getRequestURI();
            String requestPath;

            if (requestURI.startsWith("/api/storage/")) {
                requestPath = requestURI.substring("/api/storage/".length());
            } else if (requestURI.startsWith("/api/files/download/")) {
                requestPath = requestURI.substring("/api/files/download/".length());
            } else if (requestURI.startsWith("/api/storage")) {
                requestPath = requestURI.substring("/api/storage".length());
                if (requestPath.startsWith("/")) {
                    requestPath = requestPath.substring(1);
                }
            } else {
                String pathInfo = request.getPathInfo();
                requestPath = (pathInfo != null && pathInfo.startsWith("/"))
                        ? pathInfo.substring(1)
                        : (pathInfo != null ? pathInfo : "");
            }

            if (requestPath == null || requestPath.isEmpty()) {
                logger.warn("Empty request path for URI: {}", requestURI);
                return ResponseEntity.badRequest().build();
            }

            // URL decode the path
            try {
                requestPath = URLDecoder.decode(requestPath, StandardCharsets.UTF_8);
            } catch (Exception e) {
                logger.warn("Failed to decode request path '{}': {}", requestPath, e.getMessage());
                // Continue with original path if decoding fails
            }

            logger.debug("File download request - URI: {}, Path: {}", requestURI, requestPath);

            // Build full file path
            Path filePath = Paths.get(storageBasePath).resolve(requestPath).normalize();

            // Security check - ensure file is within storage directory
            Path basePath = Paths.get(storageBasePath).toAbsolutePath().normalize();
            Path normalizedFilePath = filePath.toAbsolutePath().normalize();
            if (!normalizedFilePath.startsWith(basePath)) {
                logger.warn("Path traversal attempt blocked: {} (resolved to {})", requestPath, normalizedFilePath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Check if file exists
            java.net.URI uri = filePath.toUri();
            if (uri == null) {
                logger.error("Failed to create URI for file path: {}", filePath);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            Resource resource = new UrlResource(uri);
            if (!resource.exists() || !resource.isReadable()) {
                logger.debug("File not found or not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Build response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));

            if ("true".equals(download)) {
                headers.setContentDispositionFormData("attachment", resource.getFilename());
            } else {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"");
            }

            headers.setCacheControl("public, max-age=31536000");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
