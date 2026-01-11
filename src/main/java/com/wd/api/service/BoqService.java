package com.wd.api.service;

import com.wd.api.model.BoqItem;
import com.wd.api.repository.BoqItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class BoqService {

    @Autowired
    private BoqItemRepository boqItemRepository;

    @Transactional(readOnly = true)
    public List<BoqItem> getProjectBoq(Long projectId) {
        return boqItemRepository.findByProjectId(projectId);
    }

    @Transactional
    public BoqItem updateBoqItem(Long id, BoqItem details) {
        BoqItem item = boqItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BoQ Item not found: " + id));

        item.setQuantity(details.getQuantity());
        item.setUnitRate(details.getUnitRate());
        item.setNotes(details.getNotes());
        // Trigger recalculation handled by Entity @PreUpdate/PrePersist if logic exists
        // there,
        // or manually here if needed. The entity has calculateTotalAmount() called on
        // hooks.

        return boqItemRepository.save(item);
    }

    // Note: Items are usually pre-populated or imported, but we can add create if
    // needed.
    // For now assuming viewing and updating actuals/notes.
}
