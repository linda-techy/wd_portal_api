package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.PackageDefaultCustomisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PackageDefaultCustomisationRepository extends JpaRepository<PackageDefaultCustomisation, UUID> {
}
