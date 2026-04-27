package com.wd.api.model.enums;

/**
 * Lifecycle status of a Detailed Project Costing (DPC) document.
 *
 * Flow: DRAFT -> ISSUED (terminal). Once issued the DPC is locked and
 * the rendered PDF is persisted; further edits require a new revision.
 */
public enum DpcDocumentStatus {
    DRAFT,
    ISSUED;

    public boolean isDraft() {
        return this == DRAFT;
    }

    public boolean isIssued() {
        return this == ISSUED;
    }
}
