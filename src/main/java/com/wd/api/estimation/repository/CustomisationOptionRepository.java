package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.CustomisationOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomisationOptionRepository extends JpaRepository<CustomisationOption, UUID> {
}
