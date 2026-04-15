package com.wd.api.controller;

import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.PortalNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for portal staff in-app notifications.
 *
 * GET  /api/portal/notifications?page=0&size=20   — paginated notification list + unread count
 * GET  /api/portal/notifications/unread-count      — badge count only
 * PUT  /api/portal/notifications/{id}/read         — mark one as read
 * PUT  /api/portal/notifications/read-all          — mark all as read
 */
@RestController
@RequestMapping("/api/portal/notifications")
@PreAuthorize("isAuthenticated()")
public class PortalNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(PortalNotificationController.class);

    @Autowired
    private PortalNotificationService notificationService;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.ok(notificationService.getNotifications(userId, page, size));
        } catch (Exception e) {
            logger.error("Failed to fetch portal notifications for {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch notifications"));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(userId)));
        } catch (Exception e) {
            logger.error("Failed to get unread count for {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get unread count"));
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            notificationService.markRead(id, userId);
            return ResponseEntity.ok(Map.of("message", "Marked as read"));
        } catch (Exception e) {
            logger.error("Failed to mark notification {} as read: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark notification as read"));
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            notificationService.markAllRead(userId);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (Exception e) {
            logger.error("Failed to mark all notifications as read for {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark notifications as read"));
        }
    }

    private Long getUserId(Authentication authentication) {
        return portalUserRepository.findByEmail(authentication.getName())
                .map(PortalUser::getId)
                .orElse(null);
    }
}
