package com.wd.api.repository;

import com.wd.api.model.MaterialIndentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialIndentItemRepository extends JpaRepository<MaterialIndentItem, Long> {

    List<MaterialIndentItem> findByIndentId(Long indentId);
}
