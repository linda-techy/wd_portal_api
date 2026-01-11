package com.wd.api.repository;

import com.wd.api.model.CustomerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRoleRepository extends JpaRepository<CustomerRole, Long> {
    Optional<CustomerRole> findByName(String name);
}
