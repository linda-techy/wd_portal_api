package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.CustomisationCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomisationCategoryRepository extends JpaRepository<CustomisationCategory, UUID> {
}
