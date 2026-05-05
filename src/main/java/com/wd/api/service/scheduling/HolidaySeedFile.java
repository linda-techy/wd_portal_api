package com.wd.api.service.scheduling;

import java.time.LocalDate;
import java.util.List;

/** Shape of a single kerala-YYYY.yaml file. Used by Jackson YAML. */
public record HolidaySeedFile(int year, List<Entry> holidays) {
    public record Entry(
            String code,
            String name,
            LocalDate date,
            String scope,
            String scopeRef,
            String recurrence) { }
}
