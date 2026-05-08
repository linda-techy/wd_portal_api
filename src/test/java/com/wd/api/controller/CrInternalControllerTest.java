package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.model.ProjectVariation;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.security.InternalHmacVerifier;
import com.wd.api.service.ProjectVariationService;
import com.wd.api.service.scheduling.OtpRateLimitException;
import com.wd.api.service.scheduling.OtpService;
import com.wd.api.service.scheduling.OtpVerifyOutcome;
import com.wd.api.service.scheduling.OtpVerifyResult;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser
class CrInternalControllerTest extends TestcontainersPostgresBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @MockBean InternalHmacVerifier hmac;
    @MockBean OtpService otpService;
    @MockBean ProjectVariationService projectVariationService;
    @MockBean ProjectVariationRepository projectVariationRepository;

    @BeforeEach
    void setUp() {
        when(hmac.verify(anyString(), anyString())).thenReturn(true);
        ProjectVariation cr = new ProjectVariation();
        cr.setId(42L);
        when(projectVariationRepository.findById(42L)).thenReturn(Optional.of(cr));
    }

    @Test
    void requestOtpReturns200WhenSignatureValid() throws Exception {
        mvc.perform(post("/internal/cr-request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Portal-Signature", "sha256=ok")
                .content(json.writeValueAsString(Map.of("crId", 42, "customerUserId", 7))))
            .andExpect(status().isOk());
        verify(otpService).generateForCrApproval(eq(42L), eq(7L), any());
    }

    @Test
    void requestOtpReturns401WhenSignatureInvalid() throws Exception {
        when(hmac.verify(anyString(), anyString())).thenReturn(false);
        mvc.perform(post("/internal/cr-request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Portal-Signature", "sha256=bad")
                .content(json.writeValueAsString(Map.of("crId", 42, "customerUserId", 7))))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(otpService);
    }

    @Test
    void requestOtpReturns429WhenRateLimited() throws Exception {
        doThrow(new OtpRateLimitException("rate-limited", 3600L))
            .when(otpService).generateForCrApproval(any(), any(), any());

        mvc.perform(post("/internal/cr-request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Portal-Signature", "sha256=ok")
                .content(json.writeValueAsString(Map.of("crId", 42, "customerUserId", 7))))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("RATE_LIMITED"))
            .andExpect(jsonPath("$.retryAfterSeconds").value(3600));
    }

    @Test
    void approveReturnsVerifiedAndTriggersCustomerApproval() throws Exception {
        when(otpService.verify(eq("CR_APPROVAL"), eq(42L), eq(7L), eq("123456")))
            .thenReturn(new OtpVerifyOutcome(OtpVerifyResult.VERIFIED, "hash-x"));

        mvc.perform(post("/internal/cr-approve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Portal-Signature", "sha256=ok")
                .content(json.writeValueAsString(Map.of(
                    "crId", 42, "customerUserId", 7,
                    "otpCode", "123456", "customerIp", "1.2.3.4"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VERIFIED"));

        verify(projectVariationService).approveByCustomer(eq(42L), eq(7L), eq("hash-x"), eq("1.2.3.4"));
    }

    @Test
    void approveReturnsWrongCodeAndDoesNotTransition() throws Exception {
        when(otpService.verify(any(), any(), any(), any()))
            .thenReturn(new OtpVerifyOutcome(OtpVerifyResult.WRONG_CODE, "hash-x"));

        mvc.perform(post("/internal/cr-approve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Portal-Signature", "sha256=ok")
                .content(json.writeValueAsString(Map.of(
                    "crId", 42, "customerUserId", 7,
                    "otpCode", "999999", "customerIp", "1.2.3.4"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WRONG_CODE"));

        verify(projectVariationService, never()).approveByCustomer(any(), any(), any(), any());
    }

    @Test
    void approveReturnsExpiredFromVerify() throws Exception {
        when(otpService.verify(any(), any(), any(), any()))
            .thenReturn(new OtpVerifyOutcome(OtpVerifyResult.EXPIRED, "hash-x"));
        mvc.perform(post("/internal/cr-approve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Portal-Signature", "sha256=ok")
                .content(json.writeValueAsString(Map.of(
                    "crId", 42, "customerUserId", 7,
                    "otpCode", "123456", "customerIp", "1.2.3.4"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXPIRED"));
    }
}
