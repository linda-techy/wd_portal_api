package com.wd.api.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OtpTokenEntityTest {

    @Autowired EntityManager em;

    @Test
    void persistsAllFields() {
        OtpToken t = new OtpToken();
        t.setCodeHash("a".repeat(64));
        t.setTargetType("CR_APPROVAL");
        t.setTargetId(42L);
        t.setCustomerUserId(7L);
        t.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        em.persist(t);
        em.flush();
        em.clear();

        OtpToken loaded = em.find(OtpToken.class, t.getId());
        assertThat(loaded.getCodeHash()).hasSize(64);
        assertThat(loaded.getTargetType()).isEqualTo("CR_APPROVAL");
        assertThat(loaded.getTargetId()).isEqualTo(42L);
        assertThat(loaded.getCustomerUserId()).isEqualTo(7L);
        assertThat(loaded.getAttempts()).isZero();
        assertThat(loaded.getMaxAttempts()).isEqualTo(3);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUsedAt()).isNull();
    }
}
