package com.wd.api.repository;

import com.wd.api.model.CustomerProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerProjectRepository extends JpaRepository<CustomerProject, Long> {
    List<CustomerProject> findByLeadId(Long leadId);

    List<CustomerProject> findByCustomerId(Long customerId);

    int countByCustomerId(Long customerId);

    Optional<CustomerProject> findByCode(String code);

    // Search with pagination
    @NonNull
    Page<CustomerProject> findByNameContainingIgnoreCaseOrLocationContainingIgnoreCaseOrStateContainingIgnoreCaseOrProjectPhaseContainingIgnoreCase(
            @NonNull String name, @NonNull String location, @NonNull String state, @NonNull String projectPhase,
            @NonNull Pageable pageable);

    @NonNull
    Page<CustomerProject> findAll(
            @NonNull Pageable pageable);
}
