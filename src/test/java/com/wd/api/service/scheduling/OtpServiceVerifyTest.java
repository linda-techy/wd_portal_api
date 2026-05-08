package com.wd.api.service.scheduling;

import com.wd.api.model.OtpToken;
import com.wd.api.repository.OtpTokenRepository;
import com.wd.api.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServiceVerifyTest {

    @Mock OtpTokenRepository repo;
    @Mock EmailService emailService;
    @Mock CustomerUserLookup lookup;
    @InjectMocks OtpService service;

    @Test
    void verifyReturnsNoActiveTokenWhenRepoEmpty() {
        when(repo.findActive("CR_APPROVAL", 42L, 7L)).thenReturn(Optional.empty());
        OtpVerifyOutcome outcome = service.verify("CR_APPROVAL", 42L, 7L, "123456");
        assertThat(outcome.result()).isEqualTo(OtpVerifyResult.NO_ACTIVE_TOKEN);
        assertThat(outcome.hash()).isNull();
    }

    @Test
    void verifyReturnsExpiredAndDoesNotMarkUsed() {
        OtpToken t = token("123456");
        t.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(repo.findActive(any(), any(), any())).thenReturn(Optional.of(t));

        OtpVerifyOutcome outcome = service.verify("CR_APPROVAL", 42L, 7L, "123456");
        assertThat(outcome.result()).isEqualTo(OtpVerifyResult.EXPIRED);
        assertThat(outcome.hash()).isEqualTo(sha256("123456"));
        assertThat(t.getUsedAt()).isNull();
        verify(repo, never()).save(any());
    }

    @Test
    void verifyReturnsVerifiedAndMarksUsedOnCorrectCode() {
        OtpToken t = token("123456");
        when(repo.findActive(any(), any(), any())).thenReturn(Optional.of(t));

        OtpVerifyOutcome outcome = service.verify("CR_APPROVAL", 42L, 7L, "123456");
        assertThat(outcome.result()).isEqualTo(OtpVerifyResult.VERIFIED);
        assertThat(outcome.hash()).isEqualTo(sha256("123456"));
        assertThat(t.getUsedAt()).isNotNull();
        verify(repo).save(t);
    }

    @Test
    void verifyReturnsWrongCodeAndIncrementsAttempts() {
        OtpToken t = token("123456");
        when(repo.findActive(any(), any(), any())).thenReturn(Optional.of(t));

        OtpVerifyOutcome outcome = service.verify("CR_APPROVAL", 42L, 7L, "999999");
        assertThat(outcome.result()).isEqualTo(OtpVerifyResult.WRONG_CODE);
        assertThat(outcome.hash()).isEqualTo(sha256("123456"));
        assertThat(t.getAttempts()).isEqualTo(1);
        assertThat(t.getUsedAt()).isNull();
    }

    @Test
    void verifyReturnsMaxAttemptsOnThirdWrong() {
        OtpToken t = token("123456");
        t.setAttempts(2);
        when(repo.findActive(any(), any(), any())).thenReturn(Optional.of(t));

        OtpVerifyOutcome outcome = service.verify("CR_APPROVAL", 42L, 7L, "999999");
        assertThat(outcome.result()).isEqualTo(OtpVerifyResult.MAX_ATTEMPTS);
        assertThat(outcome.hash()).isEqualTo(sha256("123456"));
        assertThat(t.getAttempts()).isEqualTo(3);
    }

    @Test
    void verifyReturnsMaxAttemptsWhenAlreadyAtCeiling() {
        OtpToken t = token("123456");
        t.setAttempts(3);
        when(repo.findActive(any(), any(), any())).thenReturn(Optional.of(t));
        OtpVerifyOutcome outcome = service.verify("CR_APPROVAL", 42L, 7L, "123456");
        assertThat(outcome.result()).isEqualTo(OtpVerifyResult.MAX_ATTEMPTS);
        assertThat(outcome.hash()).isEqualTo(sha256("123456"));
    }

    @Test
    void verifyReturnedHashIsCapturedBeforeStateMutation() {
        // The hash returned from verify() should be the value of code_hash at the
        // moment of the check, even after we set used_at on the row. This is the
        // anti-race property the redesign exists to provide.
        OtpToken t = token("123456");
        String originalHash = t.getCodeHash();
        when(repo.findActive(any(), any(), any())).thenReturn(Optional.of(t));

        OtpVerifyOutcome outcome = service.verify("CR_APPROVAL", 42L, 7L, "123456");
        assertThat(outcome.result()).isEqualTo(OtpVerifyResult.VERIFIED);
        assertThat(outcome.hash()).isEqualTo(originalHash);
        // After verify, the row is consumed (used_at set). A separate lookup
        // would no longer find an active row — but our outcome already has the hash.
        assertThat(t.getUsedAt()).isNotNull();
    }

    private OtpToken token(String plaintext) {
        OtpToken t = new OtpToken();
        t.setId(101L);
        t.setCodeHash(sha256(plaintext));
        t.setTargetType("CR_APPROVAL");
        t.setTargetId(42L);
        t.setCustomerUserId(7L);
        t.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        t.setMaxAttempts(3);
        return t;
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
