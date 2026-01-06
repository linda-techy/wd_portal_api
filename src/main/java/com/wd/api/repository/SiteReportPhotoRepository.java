package com.wd.api.repository;

import com.wd.api.model.SiteReportPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteReportPhotoRepository extends JpaRepository<SiteReportPhoto, Long> {
}
