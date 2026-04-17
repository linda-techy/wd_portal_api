package com.wd.api.service;

import com.wd.api.model.CustomerNotification;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.ProjectMember;
import com.wd.api.repository.CustomerNotificationRepository;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.repository.ProjectMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Facade for sending in-app notifications to customer-side users from portal-side events.
 *
 * Portal API and Customer API share the same PostgreSQL database, so this service
 * writes directly to the customer_notifications table and also dispatches FCM push
 * notifications to the affected devices.
 *
 * Notification strategy:
 *  - notifyOwners()   → CUSTOMER + CUSTOMER_ADMIN role members only (financial events: payments, site reports)
 *  - notifyAll()      → all customer members regardless of role (milestone completions, new documents)
 *  - notifyCustomer() → single customer user by ID, no project context (lead status changes)
 *
 * Design: runs in a separate transaction (REQUIRES_NEW) so a notification failure
 * never rolls back the caller's business transaction.
 */
@Service
public class CustomerNotificationFacade {

    private static final Logger logger = LoggerFactory.getLogger(CustomerNotificationFacade.class);

    // Roles that should receive financial/ownership-level notifications
    private static final Set<String> OWNER_ROLES = Set.of("CUSTOMER", "CUSTOMER_ADMIN", "ADMIN");

    private final ProjectMemberRepository projectMemberRepository;
    private final CustomerNotificationRepository customerNotificationRepository;
    private final CustomerUserRepository customerUserRepository;
    private final PushNotificationService pushNotificationService;

    public CustomerNotificationFacade(
            ProjectMemberRepository projectMemberRepository,
            CustomerNotificationRepository customerNotificationRepository,
            CustomerUserRepository customerUserRepository,
            PushNotificationService pushNotificationService) {
        this.projectMemberRepository = projectMemberRepository;
        this.customerNotificationRepository = customerNotificationRepository;
        this.customerUserRepository = customerUserRepository;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Notify CUSTOMER + CUSTOMER_ADMIN role members of a project.
     * Use for: site reports created, payment status updates.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOwners(Long projectId, String title, String body,
                              String notificationType, Long referenceId) {
        try {
            List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
            List<CustomerUser> targets = members.stream()
                    .filter(m -> m.getCustomerUser() != null)
                    .map(ProjectMember::getCustomerUser)
                    .filter(u -> u.getRole() != null && OWNER_ROLES.contains(u.getRole().getName()))
                    .collect(Collectors.toList());

            persistAndPush(targets, projectId, title, body, notificationType, referenceId);
        } catch (Exception e) {
            logger.error("Failed to send owner notifications for project {}: {}", projectId, e.getMessage(), e);
        }
    }

    /**
     * Notify ALL customer members of a project regardless of role.
     * Use for: milestone completions, new document uploads.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAll(Long projectId, String title, String body,
                           String notificationType, Long referenceId) {
        try {
            List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
            List<CustomerUser> targets = members.stream()
                    .filter(m -> m.getCustomerUser() != null)
                    .map(ProjectMember::getCustomerUser)
                    .collect(Collectors.toList());

            persistAndPush(targets, projectId, title, body, notificationType, referenceId);
        } catch (Exception e) {
            logger.error("Failed to send notifications to all members for project {}: {}", projectId, e.getMessage(), e);
        }
    }

    /**
     * Notify a single customer user directly by their ID.
     * Use for: lead status changes where there is no project context yet.
     *
     * @param customerUserId   ID of the CustomerUser to notify
     * @param title            notification title
     * @param body             notification body
     * @param notificationType notification type string (e.g. "LEAD_STATUS")
     * @param referenceId      ID of the linked entity (e.g. lead ID)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyCustomer(Long customerUserId, String title, String body,
                               String notificationType, Long referenceId) {
        if (customerUserId == null) return;
        try {
            CustomerUser user = customerUserRepository.findById(customerUserId).orElse(null);
            if (user == null) {
                logger.warn("notifyCustomer: CustomerUser {} not found — skipping notification", customerUserId);
                return;
            }
            persistAndPush(List.of(user), null, title, body, notificationType, referenceId);
        } catch (Exception e) {
            logger.error("Failed to send notification to customer user {}: {}", customerUserId, e.getMessage(), e);
        }
    }

    private void persistAndPush(List<CustomerUser> targets, Long projectId,
                                  String title, String body,
                                  String notificationType, Long referenceId) {
        if (targets.isEmpty()) return;

        // Persist in-app notification rows
        for (CustomerUser user : targets) {
            try {
                CustomerNotification notification = new CustomerNotification(
                        user, projectId, title, body, notificationType, referenceId);
                customerNotificationRepository.save(notification);
            } catch (Exception e) {
                logger.warn("Failed to persist notification for customer user {}: {}", user.getId(), e.getMessage());
            }
        }

        // Fire FCM push to all devices (fire-and-forget — PushNotificationService never throws)
        List<String> tokens = targets.stream()
                .map(CustomerUser::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());

        if (!tokens.isEmpty()) {
            // Build data payload — avoid Map.of() because projectId may be null
            Map<String, String> data = new HashMap<>();
            data.put("type", notificationType);
            data.put("projectId", projectId != null ? String.valueOf(projectId) : "");
            data.put("referenceId", referenceId != null ? String.valueOf(referenceId) : "");

            pushNotificationService.sendToTokens(tokens, title, body, data);
        }

        logger.info("Customer notification '{}' dispatched to {} user(s) for project {}",
                notificationType, targets.size(), projectId);
    }
}
