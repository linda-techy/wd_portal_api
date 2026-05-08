package com.wd.api.service;

import com.wd.api.model.ProjectVariation;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmailServiceCrOtpTest {

    private JavaMailSender mailSender;
    private EmailService svc;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class, RETURNS_DEEP_STUBS));
        svc = new EmailService();
        ReflectionTestUtils.setField(svc, "mailSender", mailSender);
        ReflectionTestUtils.setField(svc, "emailEnabled", true);
        ReflectionTestUtils.setField(svc, "fromEmail", "noreply@walldotbuilders.com");
        ReflectionTestUtils.setField(svc, "adminEmail", "info@walldotbuilders.com");
    }

    @Test
    void sendCrApprovalOtpInvokesMailSenderOnce() {
        ProjectVariation cr = new ProjectVariation();
        cr.setId(42L);
        cr.setDescription("Add 2 extra rooms on first floor");
        cr.setEstimatedAmount(new BigDecimal("125000.00"));

        svc.sendCrApprovalOtp("ravi@example.com", "Ravi Kumar", "123456", cr, 15);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendCrApprovalOtpFallsBackToSimulationWhenDisabled() {
        ReflectionTestUtils.setField(svc, "emailEnabled", false);
        ProjectVariation cr = new ProjectVariation();
        cr.setId(42L);
        cr.setDescription("Whatever");
        cr.setEstimatedAmount(new BigDecimal("100"));

        svc.sendCrApprovalOtp("a@b.com", "Name", "999000", cr, 15);

        verifyNoInteractions(mailSender);
    }

    @Test
    void renderedHtmlContainsOtpAndCustomerName() {
        String html = svc.buildCrApprovalOtpHtml("Ravi Kumar", "Add 2 extra rooms",
                "+\u20b91,25,000", "12 working days", "123456", 15);
        assertThat(html).contains("Ravi Kumar")
                        .contains("Add 2 extra rooms")
                        .contains("123456")
                        .contains("+\u20b91,25,000")
                        .contains("12 working days")
                        .contains("15");
    }

    @Test
    void renderedPlainTextContainsAllFields() {
        String txt = svc.buildCrApprovalOtpPlainText("Ravi Kumar", "Add 2 extra rooms",
                "Add 2 extra rooms on first floor",
                "+\u20b91,25,000", "12 working days", "123456", 15);
        assertThat(txt).contains("Ravi Kumar")
                       .contains("Add 2 extra rooms")
                       .contains("Add 2 extra rooms on first floor")
                       .contains("123456")
                       .contains("+\u20b91,25,000")
                       .contains("12 working days")
                       .contains("15 minutes");
    }
}
