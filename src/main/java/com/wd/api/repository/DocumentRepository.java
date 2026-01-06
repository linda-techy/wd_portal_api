package com.wd.api.repository;

import com.wd.api.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByReferenceIdAndReferenceTypeAndIsActiveTrue(Long referenceId, String referenceType);

    List<Document> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);
}
