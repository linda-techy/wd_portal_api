package com.wd.api.repository;

import com.wd.api.model.CustomerProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerProjectRepository extends JpaRepository<CustomerProject, Long> {
    List<CustomerProject> findByLeadId(Long leadId);
    List<CustomerProject> findByCustomerId(Long customerId);
    Optional<CustomerProject> findByCode(String code);
}

