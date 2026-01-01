package com.wd.api.repository;

import com.wd.api.model.ProjectInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectInvoiceRepository extends JpaRepository<ProjectInvoice, Long> {
    List<ProjectInvoice> findByProjectId(Long projectId);
}
