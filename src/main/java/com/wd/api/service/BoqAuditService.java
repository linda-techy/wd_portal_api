package com.wd.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.model.BoqAuditLog;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.BoqAuditLogRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * BOQ Audit Service - Logs all BOQ operations for compliance and traceability.
 * All log writes use REQUIRES_NEW propagation to ensure they're committed
 * even if the parent transaction rolls back.
 */
@Service
public class BoqAuditService {

    private static final Logger logger = LoggerFactory.getLogger(BoqAuditService.class);

    private final BoqAuditLogRepository auditLogRepository;
    private final CustomerProjectRepository projectRepository;
    private final PortalUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public BoqAuditService(BoqAuditLogRepository auditLogRepository,
                           CustomerProjectRepository projectRepository,
                           PortalUserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String entityType, Long entityId, Long projectId, Long userId, Object newValue) {
        try {
            String newValueJson = objectMapper.writeValueAsString(newValue);
            saveAuditLog(entityType, entityId, projectId, "CREATE", null, newValueJson, userId);
        } catch (Exception e) {
            logger.error("Failed to log CREATE audit for {} {}", entityType, entityId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityType, Long entityId, Long projectId, Long userId, 
                          Object oldValue, Object newValue) {
        try {
            String oldValueJson = objectMapper.writeValueAsString(oldValue);
            String newValueJson = objectMapper.writeValueAsString(newValue);
            saveAuditLog(entityType, entityId, projectId, "UPDATE", oldValueJson, newValueJson, userId);
        } catch (Exception e) {
            logger.error("Failed to log UPDATE audit for {} {}", entityType, entityId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityType, Long entityId, Long projectId, Long userId) {
        try {
            saveAuditLog(entityType, entityId, projectId, "DELETE", null, null, userId);
        } catch (Exception e) {
            logger.error("Failed to log DELETE audit for {} {}", entityType, entityId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logApprove(String entityType, Long entityId, Long projectId, Long userId) {
        try {
            saveAuditLog(entityType, entityId, projectId, "APPROVE", null, null, userId);
        } catch (Exception e) {
            logger.error("Failed to log APPROVE audit for {} {}", entityType, entityId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLock(String entityType, Long entityId, Long projectId, Long userId) {
        try {
            saveAuditLog(entityType, entityId, projectId, "LOCK", null, null, userId);
        } catch (Exception e) {
            logger.error("Failed to log LOCK audit for {} {}", entityType, entityId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logExecute(String entityType, Long entityId, Long projectId, Long userId, BigDecimal quantity) {
        try {
            Map<String, Object> value = new HashMap<>();
            value.put("executedQuantity", quantity);
            String valueJson = objectMapper.writeValueAsString(value);
            saveAuditLog(entityType, entityId, projectId, "EXECUTE", null, valueJson, userId);
        } catch (Exception e) {
            logger.error("Failed to log EXECUTE audit for {} {}", entityType, entityId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBill(String entityType, Long entityId, Long projectId, Long userId, BigDecimal quantity) {
        try {
            Map<String, Object> value = new HashMap<>();
            value.put("billedQuantity", quantity);
            String valueJson = objectMapper.writeValueAsString(value);
            saveAuditLog(entityType, entityId, projectId, "BILL", null, valueJson, userId);
        } catch (Exception e) {
            logger.error("Failed to log BILL audit for {} {}", entityType, entityId, e);
        }
    }

    private void saveAuditLog(String entityType, Long entityId, Long projectId, String actionType,
                              String oldValue, String newValue, Long userId) {
        CustomerProject project = projectId != null ? projectRepository.findById(projectId).orElse(null) : null;
        PortalUser user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        BoqAuditLog log = new BoqAuditLog(entityType, entityId, project, actionType, oldValue, newValue, user);
        auditLogRepository.save(log);

        logger.info("BOQ audit: {} {} {} by user {}", actionType, entityType, entityId, userId);
    }
}
