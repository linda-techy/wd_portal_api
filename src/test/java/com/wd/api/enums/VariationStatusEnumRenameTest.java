package com.wd.api.enums;

import com.wd.api.model.enums.VariationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariationStatusEnumRenameTest {

    @Test
    void enumHasCustomerApprovalPending_notLegacyPendingApproval() {
        assertThat(VariationStatus.CUSTOMER_APPROVAL_PENDING.name())
                .isEqualTo("CUSTOMER_APPROVAL_PENDING");
        assertThatThrownBy(() -> VariationStatus.valueOf("PENDING_APPROVAL"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enumExposesAllNineStates() {
        assertThat(VariationStatus.values()).containsExactlyInAnyOrder(
                VariationStatus.DRAFT,
                VariationStatus.SUBMITTED,
                VariationStatus.COSTED,
                VariationStatus.CUSTOMER_APPROVAL_PENDING,
                VariationStatus.APPROVED,
                VariationStatus.SCHEDULED,
                VariationStatus.IN_PROGRESS,
                VariationStatus.COMPLETE,
                VariationStatus.REJECTED);
    }
}
