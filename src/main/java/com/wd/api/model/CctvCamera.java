package com.wd.api.model;

import com.wd.api.model.enums.StreamProtocol;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cctv_cameras")
@SQLDelete(sql = "UPDATE cctv_cameras SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CctvCamera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "camera_name", nullable = false)
    private String cameraName;

    @Column(name = "location")
    private String location;

    @Column(name = "provider", length = 100)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "stream_protocol", nullable = false, length = 20)
    @Builder.Default
    private StreamProtocol streamProtocol = StreamProtocol.HLS;

    @Column(name = "stream_url", length = 1000)
    private String streamUrl;

    @Column(name = "snapshot_url", length = 1000)
    private String snapshotUrl;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "port")
    private Integer port;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "resolution", length = 50)
    private String resolution;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
