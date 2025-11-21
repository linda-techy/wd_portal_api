package com.wd.api.repository;

import com.wd.api.model.PortalRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortalRoleRepository extends JpaRepository<PortalRole, Long> {
    Optional<PortalRole> findByName(String name);
    Optional<PortalRole> findByCode(String code);
}

