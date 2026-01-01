package com.wd.api.repository;

import com.wd.api.model.MeasurementBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MeasurementBookRepository extends JpaRepository<MeasurementBook, Long> {
    List<MeasurementBook> findByProjectId(Long projectId);

    List<MeasurementBook> findByBoqItemId(Long boqItemId);
}
