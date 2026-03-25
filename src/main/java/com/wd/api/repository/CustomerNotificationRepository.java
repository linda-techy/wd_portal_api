package com.wd.api.repository;

import com.wd.api.model.CustomerNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {
}
