package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.Addon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AddonRepository extends JpaRepository<Addon, UUID> {
}
