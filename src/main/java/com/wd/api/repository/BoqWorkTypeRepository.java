package com.wd.api.repository;

import com.wd.api.model.BoqWorkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoqWorkTypeRepository extends JpaRepository<BoqWorkType, Long> {
    Optional<BoqWorkType> findByName(String name);
}
