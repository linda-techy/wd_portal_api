package com.wd.api.event;

import org.springframework.context.ApplicationEvent;

/**
 * Published when a payment stage is certified (signed off by PM or Director).
 * Triggers: trigger progress-tranche invoicing on any CO payment schedules linked to this stage.
 */
public class StagePaymentCertifiedEvent extends ApplicationEvent {

    private final Long stageId;
    private final Long projectId;
    private final String certifiedBy;

    public StagePaymentCertifiedEvent(Object source, Long stageId, Long projectId, String certifiedBy) {
        super(source);
        this.stageId     = stageId;
        this.projectId   = projectId;
        this.certifiedBy = certifiedBy;
    }

    public Long getStageId()      { return stageId; }
    public Long getProjectId()    { return projectId; }
    public String getCertifiedBy() { return certifiedBy; }
}
