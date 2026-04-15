package com.wd.api.service;

import com.wd.api.model.DesignPackagePayment;
import com.wd.api.repository.DesignPackagePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    private final DesignPackagePaymentRepository designPaymentRepository;

    /**
     * Nightly check (9 AM IST) — marks eligible payments as PENDING_RELEASE
     * so the admin dashboard can surface them for approval.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void flagEligibleRetentions() {
        LocalDate today = LocalDate.now();
        List<DesignPackagePayment> eligible = designPaymentRepository.findEligibleForRetentionRelease(today);
        if (eligible.isEmpty()) {
            return;
        }
        log.info("Retention release check: {} payment(s) eligible for release", eligible.size());
        for (DesignPackagePayment payment : eligible) {
            payment.setRetentionStatus("PENDING_RELEASE");
            designPaymentRepository.save(payment);
            log.info("Marked design payment id={} (project={}) as PENDING_RELEASE",
                    payment.getId(), payment.getProjectId());
        }
    }

    /**
     * Release retention for a specific design package payment.
     * Call this after approval has been confirmed.
     *
     * @param designPaymentId the ID of the DesignPackagePayment to release
     * @return the updated DesignPackagePayment
     */
    @Transactional
    public DesignPackagePayment releaseRetention(Long designPaymentId) {
        DesignPackagePayment payment = designPaymentRepository.findById(designPaymentId)
                .orElseThrow(() -> new RuntimeException("Design package payment not found: " + designPaymentId));

        if ("RELEASED".equals(payment.getRetentionStatus())) {
            throw new IllegalStateException("Retention has already been fully released for payment id=" + designPaymentId);
        }
        if (payment.getRetentionAmount() == null || payment.getRetentionAmount().signum() == 0) {
            throw new IllegalStateException("No retention amount configured for payment id=" + designPaymentId);
        }

        payment.setRetentionReleasedAmount(payment.getRetentionAmount());
        payment.setRetentionStatus("RELEASED");
        return designPaymentRepository.save(payment);
    }
}
