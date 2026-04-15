package com.wd.api.repository;

import com.wd.api.model.PartnershipUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnershipUserRepository extends JpaRepository<PartnershipUser, Long> {

    Optional<PartnershipUser> findByPhone(String phone);

    Optional<PartnershipUser> findByEmail(String email);

    Optional<PartnershipUser> findByPhoneOrEmail(String phone, String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    // ── Admin queries ────────────────────────────────────────────────

    Page<PartnershipUser> findByStatus(String status, Pageable pageable);

    Page<PartnershipUser> findByPartnershipType(String partnershipType, Pageable pageable);

    @Query("""
        SELECT p FROM PartnershipUser p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:partnershipType IS NULL OR p.partnershipType = :partnershipType)
          AND (:search IS NULL OR :search = ''
               OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR p.phone LIKE CONCAT('%', :search, '%')
               OR LOWER(COALESCE(p.firmName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.companyName, '')) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<PartnershipUser> searchPartners(
        @Param("status") String status,
        @Param("partnershipType") String partnershipType,
        @Param("search") String search,
        Pageable pageable
    );

    long countByStatus(String status);

    List<PartnershipUser> findByPartnershipTypeAndStatus(String partnershipType, String status);
}

