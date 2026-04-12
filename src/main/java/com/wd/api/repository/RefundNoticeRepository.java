package com.wd.api.repository;

import com.wd.api.model.RefundNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundNoticeRepository extends JpaRepository<RefundNotice, Long> {

    List<RefundNotice> findByProjectIdOrderByIssuedAtDesc(Long projectId);

    long countByProjectId(Long projectId);
}
