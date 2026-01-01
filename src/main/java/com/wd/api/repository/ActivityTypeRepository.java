package com.wd.api.repository;

import com.wd.api.model.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ActivityTypeRepository extends JpaRepository<ActivityType, Long> {
    Optional<ActivityType> findByName(String name);
}
