package com.wd.api.service;

import com.wd.api.model.PortalNotification;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalNotificationRepository;
import com.wd.api.repository.PortalUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortalNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PortalNotificationService.class);

    @Autowired
    private PortalNotificationRepository notificationRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private PushNotificationService pushNotificationService;

    /**
     * Create a notification for a portal user and immediately send a push notification.
     * Fire-and-forget for push — DB record is always saved regardless of FCM result.
     */
    @Transactional
    public void createAndPush(Long portalUserId, String title, String body,
                               String type, Long referenceId, Long projectId, Long leadId) {
        PortalUser user = portalUserRepository.findById(portalUserId).orElse(null);
        if (user == null) {
            logger.warn("Cannot create notification — portal user {} not found", portalUserId);
            return;
        }

        PortalNotification notification = new PortalNotification();
        notification.setPortalUser(user);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setNotificationType(type);
        notification.setReferenceId(referenceId);
        notification.setProjectId(projectId);
        notification.setLeadId(leadId);
        notificationRepository.save(notification);

        // Push notification (no-op if FCM not configured)
        if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("type", type != null ? type : "GENERAL");
            if (referenceId != null) data.put("referenceId", String.valueOf(referenceId));
            if (projectId != null) data.put("projectId", String.valueOf(projectId));
            if (leadId != null) data.put("leadId", String.valueOf(leadId));
            pushNotificationService.sendToToken(user.getFcmToken(), title, body, data);
        }
    }

    /**
     * Notify all portal users whose roles have the LEAD_VIEW permission.
     * Used when a new lead is created.
     */
    @Transactional
    public void notifyUsersWithPermission(String permissionName, String title, String body,
                                          String type, Long referenceId) {
        List<PortalUser> users = portalUserRepository.findByPermissionName(permissionName);
        for (PortalUser user : users) {
            try {
                createAndPush(user.getId(), title, body, type, referenceId, null, null);
            } catch (Exception e) {
                logger.warn("Failed to notify portal user {} for event {}: {}", user.getId(), type, e.getMessage());
            }
        }
    }

    /**
     * Get paginated notifications for a user.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotifications(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<PortalNotification> pageResult = notificationRepository
                .findByPortalUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), safeSize));

        List<Map<String, Object>> items = pageResult.getContent().stream()
                .map(this::toDto)
                .toList();

        long unreadCount = notificationRepository.countByPortalUser_IdAndReadFalse(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", items);
        result.put("page", page);
        result.put("size", safeSize);
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("unreadCount", unreadCount);
        return result;
    }

    /**
     * Get just the unread notification count (for the bell badge).
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByPortalUser_IdAndReadFalse(userId);
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getPortalUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    private Map<String, Object> toDto(PortalNotification n) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", n.getId());
        dto.put("title", n.getTitle());
        dto.put("body", n.getBody());
        dto.put("type", n.getNotificationType());
        dto.put("referenceId", n.getReferenceId());
        dto.put("projectId", n.getProjectId());
        dto.put("leadId", n.getLeadId());
        dto.put("read", n.isRead());
        dto.put("createdAt", n.getCreatedAt());
        return dto;
    }
}
