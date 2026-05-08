package com.wd.api.model;

import com.wd.api.model.enums.VariationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Append-only audit row for one CR (project_variation) state transition.
 *
 * <p>Distinct from {@link ChangeOrderApprovalHistory} which serves the
 * legacy ChangeOrder VO domain. This entity is for CR v2 state-machine
 * transitions on {@link ProjectVariation}.
 *
 * <p>Append-only enforcement: only INSERTs in service code; no UPDATE
 * or DELETE methods exposed by the repository.
 */
@Entity
@Table(name = "change_request_approval_history")
public class ChangeRequestApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ProjectVariation changeRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 40)
    private VariationStatus fromStatus;  // null on the initial DRAFT-create row if used

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 40, nullable = false)
    private VariationStatus toStatus;

    @Column(name = "otp_hash", length = 64)
    private String otpHash;             // SHA-256 of OTP code (PR3 sets this on customer approve)

    @Column(name = "customer_ip", length = 64)
    private String customerIp;

    @Column(name = "actor_user_id")
    private Long actorUserId;            // portal user (internal transitions)

    @Column(name = "customer_user_id")
    private Long customerUserId;         // customer user (OTP transitions)

    @Column(name = "reason", length = 500)
    private String reason;               // populated on REJECTED transitions

    @Column(name = "action_at", nullable = false, updatable = false)
    private LocalDateTime actionAt;

    @PrePersist
    protected void onCreate() {
        if (actionAt == null) actionAt = LocalDateTime.now();
    }

    // ---- Getters / setters ----
    public Long getId() { return id; }

    public ProjectVariation getChangeRequest() { return changeRequest; }
    public void setChangeRequest(ProjectVariation changeRequest) { this.changeRequest = changeRequest; }

    public VariationStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(VariationStatus fromStatus) { this.fromStatus = fromStatus; }

    public VariationStatus getToStatus() { return toStatus; }
    public void setToStatus(VariationStatus toStatus) { this.toStatus = toStatus; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public String getCustomerIp() { return customerIp; }
    public void setCustomerIp(String customerIp) { this.customerIp = customerIp; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public Long getCustomerUserId() { return customerUserId; }
    public void setCustomerUserId(Long customerUserId) { this.customerUserId = customerUserId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getActionAt() { return actionAt; }
}
