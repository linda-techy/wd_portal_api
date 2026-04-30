package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.SiteFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SiteFeeRepository extends JpaRepository<SiteFee, UUID> {
}
