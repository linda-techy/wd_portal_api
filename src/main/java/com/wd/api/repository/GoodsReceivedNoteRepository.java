package com.wd.api.repository;

import com.wd.api.model.GoodsReceivedNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoodsReceivedNoteRepository extends JpaRepository<GoodsReceivedNote, Long> {
    List<GoodsReceivedNote> findByPurchaseOrderId(Long poId);
}
