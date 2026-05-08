package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.changerequest.ChangeRequestMergeResult;
import com.wd.api.model.ChangeRequestApprovalHistory;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.OtpToken;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectVariation;
import com.wd.api.repository.ChangeRequestApprovalHistoryRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.OtpTokenRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.security.InternalHmacVerifier;
import com.wd.api.service.EmailService;
import com.wd.api.service.ProjectVariationService;
import com.wd.api.service.changerequest.ChangeRequestMergeService;
import com.wd.api.service.scheduling.CustomerUserContact;
import com.wd.api.service.scheduling.CustomerUserLookup;
import com.wd.api.service.scheduling.OtpService;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test for the CR approval OTP round-trip.
 *
 * Exercises a real {@link OtpService}, real {@link OtpTokenRepository},
 * real {@link ProjectVariationService} and real {@link ChangeRequestApprovalHistoryRepository}
 * against a Postgres Testcontainer. Only thin shims (HMAC, email, customer
 * lookup, merge service) are mocked out.
 *
 * The blocker fix this test guards: the SHA-256 hash captured at OTP
 * verification time MUST be the value persisted into change_request_approval_history
 * for that CR's APPROVED transition. Previously the hash was looked up via a
 * separate hashFor() accessor that raced with verify()'s state mutation; the
 * new OtpVerifyOutcome captures the hash before any mutation so the audit
 * trail is correct under concurrent load.
 */
@AutoConfigureMockMvc
@WithMockUser
class CrApprovalIntegrationTest extends TestcontainersPostgresBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Autowired OtpService otpService;
    @Autowired OtpTokenRepository otpRepo;
    @Autowired ProjectVariationService projectVariationService;
    @Autowired ProjectVariationRepository variationRepo;
    @Autowired ChangeRequestApprovalHistoryRepository historyRepo;
    @Autowired CustomerProjectRepository projectRepo;
    @Autowired PortalUserRepository userRepo;

    @MockBean InternalHmacVerifier hmac;
    @MockBean EmailService emailService;
    @MockBean CustomerUserLookup customerUserLookup;
    @MockBean ChangeRequestMergeService mergeService;

    private Long projectId;
    private Long actorId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        when(hmac.verify(anyString(), anyString())).thenReturn(true);
        when(customerUserLookup.contactFor(anyLong()))
            .thenReturn(new CustomerUserContact("c@example.com", "Customer"));
        when(mergeService.mergeIntoWbs(anyLong(), anyLong(), any()))
            .thenReturn(new ChangeRequestMergeResult(0, 0, 0, true));

        // EmailService is a no-op mock — we don't care about send behaviour here,
        // just that OtpService can flow through without an SMTP error.
        doAnswer(inv -> null).when(emailService)
            .sendCrApprovalOtp(anyString(), anyString(), anyString(), any(), org.mockito.ArgumentMatchers.anyInt());

        CustomerProject p = new CustomerProject();
        p.setName("cr-otp-it-" + UUID.randomUUID());
        p.setLocation("L");
        p.setProjectUuid(UUID.randomUUID());
        projectId = projectRepo.save(p).getId();

        PortalUser u = new PortalUser();
        u.setEmail("a-" + UUID.randomUUID() + "@t");
        u.setFirstName("a"); u.setLastName("u"); u.setPassword("x"); u.setEnabled(true);
        actorId = userRepo.save(u).getId();

        PortalUser c = new PortalUser();
        c.setEmail("c-" + UUID.randomUUID() + "@t");
        c.setFirstName("c"); c.setLastName("u"); c.setPassword("x"); c.setEnabled(true);
        customerId = userRepo.save(c).getId();
    }

    @Test
    void approveFlow_capturedHashMatchesPersistedHistoryRow() throws Exception {
        // 1. Drive a CR to CUSTOMER_APPROVAL_PENDING.
        ProjectVariation draft = ProjectVariation.builder()
            .description("install upgraded chandeliers")
            .estimatedAmount(new BigDecimal("12000"))
            .build();
        ProjectVariation cr = projectVariationService.createVariation(draft, projectId, actorId);
        cr = projectVariationService.submit(cr.getId(), actorId);
        cr = projectVariationService.cost(cr.getId(), new BigDecimal("12000"), 2, actorId);
        cr = projectVariationService.sendToCustomer(cr.getId(), actorId);

        // 2. Have the real OtpService generate a token. We can't see the plaintext
        //    code from generateForCrApproval, so we plant our own token row so
        //    the verify path runs against a known plaintext.
        String plaintext = "246810";
        String expectedHash = sha256(plaintext);
        OtpToken token = new OtpToken();
        token.setCodeHash(expectedHash);
        token.setTargetType("CR_APPROVAL");
        token.setTargetId(cr.getId());
        token.setCustomerUserId(customerId);
        token.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
        otpRepo.save(token);

        // 3. POST to /internal/cr-approve with the matching plaintext code.
        mvc.perform(post("/internal/cr-approve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Portal-Signature", "sha256=ok")
                .content(json.writeValueAsString(Map.of(
                    "crId", cr.getId(),
                    "customerUserId", customerId,
                    "otpCode", plaintext,
                    "customerIp", "203.0.113.7"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VERIFIED"));

        // 4. The hash captured at verify-time must be the SHA-256 written into
        //    change_request_approval_history for the CUSTOMER_APPROVAL_PENDING ->
        //    APPROVED transition row.
        List<ChangeRequestApprovalHistory> rows =
            historyRepo.findByChangeRequestIdOrderByActionAtDesc(cr.getId());
        ChangeRequestApprovalHistory approve = rows.stream()
            .filter(r -> r.getToStatus() == com.wd.api.model.enums.VariationStatus.APPROVED)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no APPROVED row in history"));

        assertThat(approve.getOtpHash())
            .as("hash on history row must match the SHA-256 of the OTP that was verified")
            .isEqualTo(expectedHash);
        assertThat(approve.getCustomerUserId()).isEqualTo(customerId);
        assertThat(approve.getCustomerIp()).isEqualTo("203.0.113.7");
        assertThat(approve.getActorUserId()).isNull();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
