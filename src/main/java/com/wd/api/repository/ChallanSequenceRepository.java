package com.wd.api.repository;

import com.wd.api.model.ChallanSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChallanSequenceRepository extends JpaRepository<ChallanSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ChallanSequence s WHERE s.fy = :fy")
    Optional<ChallanSequence> findByFyWithLock(String fy);

    Optional<ChallanSequence> findByFy(String fy);
}
