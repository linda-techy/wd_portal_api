package com.wd.api.service.scheduling;

import com.wd.api.model.OtpToken;
import com.wd.api.model.ProjectVariation;
import com.wd.api.repository.OtpTokenRepository;
import com.wd.api.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServiceTest {

    @Mock OtpTokenRepository repo;
    @Mock EmailService emailService;
    @Mock CustomerUserLookup lookup;

    @InjectMocks OtpService service;

    ProjectVariation cr;

    @BeforeEach
    void setUp() {
        cr = new ProjectVariation();
        cr.setId(42L);
        cr.setDescription("Add 2 extra rooms");
        cr.setEstimatedAmount(new BigDecimal("125000"));
        when(repo.save(any(OtpToken.class))).thenAnswer(inv -> {
            OtpToken t = inv.getArgument(0);
            if (t.getId() == null) t.setId(101L);
            return t;
        });
        when(lookup.contactFor(7L))
            .thenReturn(new CustomerUserContact("ravi@example.com", "Ravi Kumar"));
    }

    @Test
    void generateProducesSixDigitNumericCode() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        service.generateForCrApproval(42L, 7L, cr);
        verify(emailService).sendCrApprovalOtp(eq("ravi@example.com"), eq("Ravi Kumar"),
                                                codeCaptor.capture(), eq(cr), eq(15));
        assertThat(codeCaptor.getValue()).matches("\\d{6}");
    }

    @Test
    void generateStoresSha256OfPlaintext() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);

        service.generateForCrApproval(42L, 7L, cr);

        verify(repo).save(tokenCaptor.capture());
        verify(emailService).sendCrApprovalOtp(any(), any(), codeCaptor.capture(), any(), anyInt());

        String expected = sha256(codeCaptor.getValue());
        assertThat(tokenCaptor.getValue().getCodeHash()).isEqualTo(expected);
    }

    @Test
    void generateSetsExpiry15MinutesInFuture() {
        ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
        LocalDateTime before = LocalDateTime.now();
        service.generateForCrApproval(42L, 7L, cr);
        LocalDateTime after = LocalDateTime.now();

        verify(repo).save(tokenCaptor.capture());
        LocalDateTime exp = tokenCaptor.getValue().getExpiresAt();
        assertThat(exp).isBetween(before.plusMinutes(15).minusSeconds(2),
                                  after.plusMinutes(15).plusSeconds(2));
    }

    @Test
    void generateSetsTargetTypeAndIds() {
        ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
        service.generateForCrApproval(42L, 7L, cr);
        verify(repo).save(tokenCaptor.capture());
        OtpToken t = tokenCaptor.getValue();
        assertThat(t.getTargetType()).isEqualTo("CR_APPROVAL");
        assertThat(t.getTargetId()).isEqualTo(42L);
        assertThat(t.getCustomerUserId()).isEqualTo(7L);
    }

    @Test
    void rateLimitTriggersOnSixthRequestWithin24h() {
        when(repo.countCreatedSince(eq("CR_APPROVAL"), eq(42L), eq(7L), any())).thenReturn(5L);
        assertThatThrownBy(() -> service.generateForCrApproval(42L, 7L, cr))
            .isInstanceOf(OtpRateLimitException.class)
            .hasMessageContaining("rate-limited");
        verifyNoInteractions(emailService);
        verify(repo, never()).save(any());
    }

    @Test
    void rateLimitAllowsFifthRequestWithin24h() {
        when(repo.countCreatedSince(eq("CR_APPROVAL"), eq(42L), eq(7L), any())).thenReturn(4L);
        service.generateForCrApproval(42L, 7L, cr);
        verify(emailService).sendCrApprovalOtp(any(), any(), any(), any(), anyInt());
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
