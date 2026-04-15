package com.wd.api.model;

import com.wd.api.model.enums.IdProofType;
import com.wd.api.model.enums.LabourTradeType;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import lombok.*;
import java.math.BigDecimal;

@SQLDelete(sql = "UPDATE labour SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "labour")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Labour extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false)
    private LabourTradeType tradeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_proof_type")
    private IdProofType idProofType;

    @Column(name = "id_proof_number")
    private String idProofNumber;

    @Column(name = "daily_wage", precision = 15, scale = 2, nullable = false)
    private BigDecimal dailyWage;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Builder.Default
    @Column(name = "is_active")
    private boolean active = true;

    @Override
    @PrePersist
    public void onCreate() {
        super.onCreate();
    }
}
