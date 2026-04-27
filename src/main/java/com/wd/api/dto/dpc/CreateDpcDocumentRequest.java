package com.wd.api.dto.dpc;

/**
 * Request body for creating a new DPC document.
 *
 * Empty by design — the server auto-populates everything from the project's
 * APPROVED BoQ + the active scope-template library.  This empty record exists
 * only so the create endpoint can declare an explicit body type.
 */
public record CreateDpcDocumentRequest() {
}
