package com.wd.api.repository;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.ProjectPhase;
import com.wd.api.model.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerProjectRepository extends JpaRepository<CustomerProject, Long> {

        // ==================== Legacy Methods (Maintained for Compatibility)
        // ====================

        List<CustomerProject> findByLeadId(Long leadId);

        boolean existsByLeadId(Long leadId);

        List<CustomerProject> findByCustomer_Id(Long customerId);

        int countByCustomer_Id(Long customerId);

        Optional<CustomerProject> findByCode(String code);

        // Search with pagination (note: projectPhase is now enum, method signature
        // preserved for compatibility)
        @NonNull
        Page<CustomerProject> findByNameContainingIgnoreCaseOrLocationContainingIgnoreCaseOrStateContainingIgnoreCaseOrProjectPhaseContainingIgnoreCase(
                        @NonNull String name, @NonNull String location, @NonNull String state,
                        @NonNull String projectPhase,
                        @NonNull Pageable pageable);

        @NonNull
        Page<CustomerProject> findAll(
                        @NonNull Pageable pageable);

        // ==================== Soft Delete Support ====================

        /**
         * Find all active (non-deleted) projects with pagination
         */
        Page<CustomerProject> findByDeletedAtIsNull(Pageable pageable);

        /**
         * Find active project by ID
         */
        Optional<CustomerProject> findByIdAndDeletedAtIsNull(Long id);

        /**
         * Find all active projects by customer
         */
        List<CustomerProject> findByCustomer_IdAndDeletedAtIsNull(Long customerId);

        /**
         * Count active projects for a customer
         */
        int countByCustomer_IdAndDeletedAtIsNull(Long customerId);

        /**
         * Check if an active project exists with given code
         */
        boolean existsByCodeAndDeletedAtIsNull(String code);

        // ==================== Project Manager Queries ====================

        /**
         * Find all active projects managed by a specific portal user
         */
        List<CustomerProject> findByProjectManager_IdAndDeletedAtIsNull(Long projectManagerId);

        /**
         * Find projects by manager with pagination
         */
        Page<CustomerProject> findByProjectManager_IdAndDeletedAtIsNull(Long projectManagerId, Pageable pageable);

        /**
         * Count projects for a project manager
         */
        int countByProjectManager_IdAndDeletedAtIsNull(Long projectManagerId);

        // ==================== Phase-Based Queries ====================

        /**
         * Find active projects by phase
         */
        List<CustomerProject> findByProjectPhaseAndDeletedAtIsNull(ProjectPhase phase);

        /**
         * Find active projects by customer and phase
         */
        List<CustomerProject> findByCustomer_IdAndProjectPhaseAndDeletedAtIsNull(
                        Long customerId, ProjectPhase phase);

        /**
         * Find active projects by phase with pagination
         */
        Page<CustomerProject> findByProjectPhaseAndDeletedAtIsNull(ProjectPhase phase, Pageable pageable);

        // ==================== Status-Based Queries ====================

        /**
         * Find active projects by operational status
         */
        List<CustomerProject> findByProjectStatusAndDeletedAtIsNull(ProjectStatus status);

        /**
         * Find projects by status with pagination
         */
        Page<CustomerProject> findByProjectStatusAndDeletedAtIsNull(ProjectStatus status, Pageable pageable);

        // ==================== Aggregation Queries ====================

        /**
         * Get count of projects grouped by phase
         */
        @Query("SELECT p.projectPhase, COUNT(p) FROM CustomerProject p " +
                        "WHERE p.deletedAt IS NULL GROUP BY p.projectPhase")
        List<Object[]> getProjectCountByPhase();

        /**
         * Get count of projects grouped by status
         */
        @Query("SELECT p.projectStatus, COUNT(p) FROM CustomerProject p " +
                        "WHERE p.deletedAt IS NULL GROUP BY p.projectStatus")
        List<Object[]> getProjectCountByStatus();

        /**
         * Get total budget for a customer's active projects
         */
        @Query("SELECT COALESCE(SUM(p.budget), 0) FROM CustomerProject p " +
                        "WHERE p.customer.id = :customerId AND p.deletedAt IS NULL")
        BigDecimal getTotalBudgetByCustomer(@Param("customerId") Long customerId);

        /**
         * Get total square footage for a customer's active projects
         */
        @Query("SELECT COALESCE(SUM(p.sqfeet), 0) FROM CustomerProject p " +
                        "WHERE p.customer.id = :customerId AND p.deletedAt IS NULL")
        BigDecimal getTotalSqfeetByCustomer(@Param("customerId") Long customerId);

        /**
         * Find projects within budget range (active only)
         */
        @Query("SELECT p FROM CustomerProject p " +
                        "WHERE p.budget BETWEEN :minBudget AND :maxBudget " +
                        "AND p.deletedAt IS NULL")
        Page<CustomerProject> findByBudgetBetween(
                        @Param("minBudget") BigDecimal minBudget,
                        @Param("maxBudget") BigDecimal maxBudget,
                        Pageable pageable);

        /**
         * Find overdue projects (end date passed but status not COMPLETED)
         */
        @Query("SELECT p FROM CustomerProject p " +
                        "WHERE p.endDate < CURRENT_DATE " +
                        "AND p.projectStatus != com.wd.api.model.enums.ProjectStatus.COMPLETED " +
                        "AND p.deletedAt IS NULL")
        List<CustomerProject> findOverdueProjects();

        /**
         * Advanced search with multiple criteria (active projects only)
         */
        @Query("SELECT p FROM CustomerProject p " +
                        "WHERE (:customerId IS NULL OR p.customer.id = :customerId) " +
                        "AND (:projectManagerId IS NULL OR p.projectManager.id = :projectManagerId) " +
                        "AND (:phase IS NULL OR p.projectPhase = :phase) " +
                        "AND (:status IS NULL OR p.projectStatus = :status) " +
                        "AND p.deletedAt IS NULL")
        Page<CustomerProject> searchProjects(
                        @Param("customerId") Long customerId,
                        @Param("projectManagerId") Long projectManagerId,
                        @Param("phase") ProjectPhase phase,
                        @Param("status") ProjectStatus status,
                        Pageable pageable);
}
