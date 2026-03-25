package com.wd.api.repository;

import com.wd.api.model.PortalNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortalNotificationRepository extends JpaRepository<PortalNotification, Long> {

    // Paginated notifications for a user — newest first
    Page<PortalNotification> findByPortalUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Unread count for the notification bell badge
    long countByPortalUser_IdAndReadFalse(Long userId);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE PortalNotification n SET n.read = true WHERE n.portalUser.id = :userId AND n.read = false")
    int markAllReadByUserId(@Param("userId") Long userId);
}
