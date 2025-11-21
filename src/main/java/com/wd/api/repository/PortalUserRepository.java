package com.wd.api.repository;

import com.wd.api.model.PortalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {
    Optional<PortalUser> findByEmail(String email);
}

