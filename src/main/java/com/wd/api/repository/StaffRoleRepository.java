package com.wd.api.repository;

import com.wd.api.model.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRoleRepository extends JpaRepository<StaffRole, Long> {
    
    Optional<StaffRole> findByName(String name);
    
    List<StaffRole> findAllByOrderByDisplayOrderAsc();
}
