package com.wd.api.event;

import org.springframework.context.ApplicationEvent;

/**
 * Published when a Variation Order reaches APPROVED status (final level sign-off).
 * Triggers: auto-create ChangeOrderPaymentSchedule, notify customer, update project financials.
 */
public class VOApprovedEvent extends ApplicationEvent {

    private final Long changeOrderId;
    private final Long projectId;
    private final Long approvedByUserId;

    public VOApprovedEvent(Object source, Long changeOrderId, Long projectId, Long approvedByUserId) {
        super(source);
        this.changeOrderId   = changeOrderId;
        this.projectId       = projectId;
        this.approvedByUserId = approvedByUserId;
    }

    public Long getChangeOrderId()    { return changeOrderId; }
    public Long getProjectId()        { return projectId; }
    public Long getApprovedByUserId() { return approvedByUserId; }
}
