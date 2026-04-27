package com.wd.api.repository;

import com.wd.api.model.DpcDocumentScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DpcDocumentScopeRepository extends JpaRepository<DpcDocumentScope, Long> {

    List<DpcDocumentScope> findByDpcDocumentIdOrderByDisplayOrderAsc(Long dpcDocumentId);
}
