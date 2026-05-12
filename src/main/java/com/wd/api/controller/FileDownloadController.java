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

    @Value("${storageBasePath}")
    private String storageBasePath;

    /**
     * Serve files from storage path
     * GET /api/storage/projects/1/documents/file.pdf
     * GET /api/files/download/site-reports/10/file.png (legacy path)
     *
     * Permission: any authenticated user with one of the resource-view rights
     * can fetch a file. The controller serves a generic storage tree
     * (site-reports/, gallery/, projects/.../documents/, etc.), and gating it
     * on DOCUMENT_VIEW alone broke site engineers / supervisors who can
     * create + view site reports (with embedded photos) but don't have the
     * documents permission. Auth is still required; the URLs themselves
     * carry unguessable identifiers.
     */
    @GetMapping({"/api/storage/**", "/api/files/download/**"})
    @PreAuthorize("hasAnyAuthority('DOCUMENT_VIEW', 'SITE_REPORT_VIEW', 'SITE_REPORT_CREATE', 'GALLERY_VIEW')")
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

            // G-63: defence-in-depth against path traversal. Reject obvious
            // attacks (raw `..` segments, NUL bytes, absolute paths) BEFORE
            // resolving — even though normalize+startsWith below would also
            // catch them, an early reject keeps suspicious paths out of the
            // filesystem syscall layer.
            if (requestPath.contains("\0")
                    || hasParentSegment(requestPath)
                    || isAbsolutePathInput(requestPath)) {
                logger.warn("Suspicious file-download path rejected: {}", requestPath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

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

            // G-63: symlink-safe re-check. .normalize() does NOT follow
            // symlinks — a malicious symlink under storageBasePath could
            // point at /etc/passwd. toRealPath() resolves the symlink chain;
            // we then re-verify containment under the real base.
            try {
                Path realFile = filePath.toRealPath();
                Path realBase = basePath.toRealPath();
                if (!realFile.startsWith(realBase)) {
                    logger.warn("Symlink escape blocked: {} -> {}", filePath, realFile);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            } catch (java.nio.file.NoSuchFileException nsfe) {
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

            headers.setCacheControl("private, no-store, max-age=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Returns true if any path segment is exactly "..". This catches both
     *  forward-slash and back-slash separated inputs, including encoded
     *  variants that survived URLDecoder (e.g. URLDecoder leaves "%2e%2e"
     *  alone if the request used "%252e%252e"). Package-private for unit
     *  testing — the rejection rule is security-critical enough to deserve
     *  direct coverage independent of the controller wiring. */
    static boolean hasParentSegment(String path) {
        for (String seg : path.replace('\\', '/').split("/")) {
            if ("..".equals(seg.trim())) return true;
        }
        return false;
    }

    /** Reject inputs that look like absolute paths on either POSIX or
     *  Windows (a leading "/" is OK on POSIX layouts because the controller
     *  pre-strips it; this catches drive letters and UNC roots). */
    static boolean isAbsolutePathInput(String path) {
        if (path.length() >= 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
            return true; // C:\... on Windows
        }
        if (path.startsWith("//") || path.startsWith("\\\\")) {
            return true; // \\server\share UNC
        }
        return false;
    }
}
