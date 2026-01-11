package com.wd.api.repository;

import com.wd.api.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByProjectId(Long projectId);

    List<Receipt> findByInvoiceId(Long invoiceId);
}
