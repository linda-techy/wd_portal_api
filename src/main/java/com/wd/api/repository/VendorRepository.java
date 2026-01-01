package com.wd.api.repository;

import com.wd.api.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByPhone(String phone);

    Optional<Vendor> findByEmail(String email);

    Optional<Vendor> findByGstin(String gstin);
}
