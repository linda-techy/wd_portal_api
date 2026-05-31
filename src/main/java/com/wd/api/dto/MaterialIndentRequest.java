package com.wd.api.dto;

import com.wd.api.model.MaterialIndent;
import com.wd.api.model.MaterialIndentItem;
import java.time.LocalDate;
import java.util.List;

public record MaterialIndentRequest(
        String indentNumber,
        LocalDate requestDate,
        LocalDate requiredDate,
        MaterialIndent.IndentPriority priority,
        String notes,
        List<MaterialIndentItem> items) {

    public MaterialIndent toEntity() {
        MaterialIndent indent = new MaterialIndent();
        indent.setIndentNumber(indentNumber);
        indent.setRequestDate(requestDate);
        indent.setRequiredDate(requiredDate);
        indent.setPriority(priority);
        indent.setNotes(notes);
        indent.setItems(items);
        return indent;
    }
}
