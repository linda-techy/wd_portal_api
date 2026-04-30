package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.GovtFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GovtFeeRepository extends JpaRepository<GovtFee, UUID> {
}
