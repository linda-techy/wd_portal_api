package com.wd.api.repository;

import com.wd.api.model.CustomerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRoleRepository extends JpaRepository<CustomerRole, Long> {
}

