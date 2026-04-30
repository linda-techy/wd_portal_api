package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.EstimationPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EstimationPackageRepository extends JpaRepository<EstimationPackage, UUID> {
}
