package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.SettingsAuditEntry;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SettingsAuditEntryPersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_settingsAuditEntry() {
        SettingsAuditEntry entry = new SettingsAuditEntry();
        entry.setEntityName("estimation_package_rate_version");
        entry.setEntityId(UUID.randomUUID());
        entry.setFieldName("material_rate");
        entry.setOldValue("1500.00");
        entry.setNewValue("1550.00");
        entry.setReason("Quarterly steel-price update");
        entry.setActorUserId(42L);

        em.persist(entry);
        em.flush();
        UUID id = entry.getId();
        em.clear();

        SettingsAuditEntry loaded = em.find(SettingsAuditEntry.class, id);
        assertThat(loaded.getEntityName()).isEqualTo("estimation_package_rate_version");
        assertThat(loaded.getFieldName()).isEqualTo("material_rate");
        assertThat(loaded.getOldValue()).isEqualTo("1500.00");
        assertThat(loaded.getNewValue()).isEqualTo("1550.00");
        assertThat(loaded.getActorUserId()).isEqualTo(42L);
    }
}
