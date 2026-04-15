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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BOQ Audit Service - Logs all BOQ operations for compliance and traceability.
 *
 * Uses TransactionTemplate with REQUIRES_NEW propagation instead of @Transactional
 * to ensure audit failures never roll back the caller's transaction.
 *
 * Root cause of the previous approach: @Transactional(REQUIRES_NEW) methods that
 * catch exceptions internally would get marked rollback-only by Spring before
 * the catch block ran, then throw UnexpectedRollbackException on commit attempt.
 */
@Service
public class BoqAuditService {

    private static final Logger logger = LoggerFactory.getLogger(BoqAuditService.class);

    private final BoqAuditLogRepository auditLogRepository;
    private final CustomerProjectRepository projectRepository;
    private final PortalUserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate requiresNewTx;

    public BoqAuditService(BoqAuditLogRepository auditLogRepository,
                           CustomerProjectRepository projectRepository,
                           PortalUserRepository userRepository,
                           PlatformTransactionManager txManager) {
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();

        this.requiresNewTx = new TransactionTemplate(txManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void logCreate(String entityType, Long entityId, Long projectId, Long userId, Object newValue) {
        try {
            final String newValueJson = objectMapper.writeValueAsString(newValue);
            requiresNewTx.execute(status -> {
                saveAuditLog(entityType, entityId, projectId, "CREATE", null, newValueJson, userId);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to log CREATE audit for {} {}", entityType, entityId, e);
        }
    }

    public void logUpdate(String entityType, Long entityId, Long projectId, Long userId,
                          Object oldValue, Object newValue) {
        try {
            final String oldValueJson = objectMapper.writeValueAsString(oldValue);
            final String newValueJson = objectMapper.writeValueAsString(newValue);
            requiresNewTx.execute(status -> {
                saveAuditLog(entityType, entityId, projectId, "UPDATE", oldValueJson, newValueJson, userId);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to log UPDATE audit for {} {}", entityType, entityId, e);
        }
    }

    public void logDelete(String entityType, Long entityId, Long projectId, Long userId) {
        try {
            requiresNewTx.execute(status -> {
                saveAuditLog(entityType, entityId, projectId, "DELETE", null, null, userId);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to log DELETE audit for {} {}", entityType, entityId, e);
        }
    }

    public void logApprove(String entityType, Long entityId, Long projectId, Long userId) {
        try {
            requiresNewTx.execute(status -> {
                saveAuditLog(entityType, entityId, projectId, "APPROVE", null, null, userId);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to log APPROVE audit for {} {}", entityType, entityId, e);
        }
    }

    public void logLock(String entityType, Long entityId, Long projectId, Long userId) {
        try {
            requiresNewTx.execute(status -> {
                saveAuditLog(entityType, entityId, projectId, "LOCK", null, null, userId);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to log LOCK audit for {} {}", entityType, entityId, e);
        }
    }

    public void logExecute(String entityType, Long entityId, Long projectId, Long userId, BigDecimal quantity) {
        try {
            final Map<String, Object> value = new HashMap<>();
            value.put("executedQuantity", quantity);
            final String valueJson = objectMapper.writeValueAsString(value);
            requiresNewTx.execute(status -> {
                saveAuditLog(entityType, entityId, projectId, "EXECUTE", null, valueJson, userId);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to log EXECUTE audit for {} {}", entityType, entityId, e);
        }
    }

    public void logBill(String entityType, Long entityId, Long projectId, Long userId, BigDecimal quantity) {
        try {
            final Map<String, Object> value = new HashMap<>();
            value.put("billedQuantity", quantity);
            final String valueJson = objectMapper.writeValueAsString(value);
            requiresNewTx.execute(status -> {
                saveAuditLog(entityType, entityId, projectId, "BILL", null, valueJson, userId);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to log BILL audit for {} {}", entityType, entityId, e);
        }
    }

    public List<BoqAuditLog> getAuditLogForItem(Long itemId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc("BOQ_ITEM", itemId);
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
