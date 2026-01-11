package com.wd.api.repository;

import com.wd.api.model.VendorQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorQuotationRepository extends JpaRepository<VendorQuotation, Long> {
    List<VendorQuotation> findByIndentId(Long indentId);

    List<VendorQuotation> findByVendorId(Long vendorId);
}
