package com.wd.api.model.enums;

/**
 * Origin of a DPC customization line.
 *
 * AUTO_FROM_BOQ_ADDON — line was generated from an ADDON BoQ item; will be
 *                       regenerated on demand and should not be edited by hand.
 * MANUAL              — line was added by an editor; preserved across regeneration.
 */
public enum DpcCustomizationSource {
    AUTO_FROM_BOQ_ADDON,
    MANUAL
}
