package com.wd.api.repository;

import com.wd.api.model.MaterialIndent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialIndentRepository
        extends JpaRepository<MaterialIndent, Long>, JpaSpecificationExecutor<MaterialIndent> {

    // Find Indents by Project
    List<MaterialIndent> findByProjectId(Long projectId);

    // Find Indents by Status
    List<MaterialIndent> findByStatus(MaterialIndent.IndentStatus status);

    // Check duplicate Indent Number
    boolean existsByIndentNumber(String indentNumber);
}
