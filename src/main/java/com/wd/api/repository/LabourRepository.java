package com.wd.api.repository;

import com.wd.api.model.Labour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabourRepository extends JpaRepository<Labour, Long>, JpaSpecificationExecutor<Labour> {
}
