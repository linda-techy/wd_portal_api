package com.wd.api.event;

import org.springframework.context.ApplicationEvent;

/**
 * Published when a FinalAccount transitions to AGREED status.
 * Triggers: settle all pending deduction register entries, trigger completion
 * tranches on all CO payment schedules, begin DLP countdown.
 */
public class FinalAccountAgreedEvent extends ApplicationEvent {

    private final Long finalAccountId;
    private final Long projectId;
    private final String agreedBy;

    public FinalAccountAgreedEvent(Object source, Long finalAccountId, Long projectId, String agreedBy) {
        super(source);
        this.finalAccountId = finalAccountId;
        this.projectId      = projectId;
        this.agreedBy       = agreedBy;
    }

    public Long getFinalAccountId() { return finalAccountId; }
    public Long getProjectId()      { return projectId; }
    public String getAgreedBy()     { return agreedBy; }
}
