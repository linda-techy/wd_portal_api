package com.wd.api.repository;

import com.wd.api.model.OtpToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OtpTokenRepositoryTest {

    @Autowired OtpTokenRepository repo;

    @Test
    void findActiveReturnsUnusedToken() {
        OtpToken active = save("CR_APPROVAL", 5L, 9L, LocalDateTime.now().plusMinutes(15), null);
        save("CR_APPROVAL", 5L, 9L, LocalDateTime.now().plusMinutes(15), LocalDateTime.now()); // used

        Optional<OtpToken> hit = repo.findActive("CR_APPROVAL", 5L, 9L);
        assertThat(hit).isPresent();
        assertThat(hit.get().getId()).isEqualTo(active.getId());
    }

    @Test
    void countCreatedSinceCountsRecentRequests() {
        save("CR_APPROVAL", 5L, 9L, LocalDateTime.now().plusMinutes(15), null);
        save("CR_APPROVAL", 5L, 9L, LocalDateTime.now().plusMinutes(15), null);
        save("CR_APPROVAL", 5L, 9L, LocalDateTime.now().plusMinutes(15), null);

        long n = repo.countCreatedSince("CR_APPROVAL", 5L, 9L, LocalDateTime.now().minusHours(24));
        assertThat(n).isEqualTo(3);
    }

    private OtpToken save(String type, Long targetId, Long custId, LocalDateTime expiresAt, LocalDateTime usedAt) {
        OtpToken t = new OtpToken();
        t.setCodeHash("0".repeat(64));
        t.setTargetType(type);
        t.setTargetId(targetId);
        t.setCustomerUserId(custId);
        t.setExpiresAt(expiresAt);
        t.setUsedAt(usedAt);
        return repo.save(t);
    }
}
