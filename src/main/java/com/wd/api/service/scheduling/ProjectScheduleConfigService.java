package com.wd.api.service.scheduling;

import com.wd.api.dto.scheduling.HolidayOverrideRequest;
import com.wd.api.dto.scheduling.ProjectScheduleConfigDto;
import com.wd.api.model.scheduling.ProjectHolidayOverride;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.repository.ProjectHolidayOverrideRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectScheduleConfigService {

    private final ProjectScheduleConfigRepository configRepo;
    private final ProjectHolidayOverrideRepository overrideRepo;
    private final HolidayService holidayService;

    public ProjectScheduleConfigService(ProjectScheduleConfigRepository configRepo,
                                        ProjectHolidayOverrideRepository overrideRepo,
                                        HolidayService holidayService) {
        this.configRepo = configRepo;
        this.overrideRepo = overrideRepo;
        this.holidayService = holidayService;
    }

    @Transactional(readOnly = true)
    public ProjectScheduleConfigDto get(Long projectId) {
        return configRepo.findByProjectId(projectId)
                .map(this::toDto)
                .orElseGet(() -> new ProjectScheduleConfigDto(projectId, false, (short) 601, (short) 930, null));
    }

    @Transactional
    public ProjectScheduleConfigDto upsert(Long projectId, ProjectScheduleConfigDto dto) {
        ProjectScheduleConfig cfg = configRepo.findByProjectId(projectId)
                .orElseGet(() -> {
                    ProjectScheduleConfig fresh = new ProjectScheduleConfig();
                    fresh.setProjectId(projectId);
                    return fresh;
                });
        if (dto.sundayWorking() != null) cfg.setSundayWorking(dto.sundayWorking());
        if (dto.monsoonStartMonthDay() != null) cfg.setMonsoonStartMonthDay(dto.monsoonStartMonthDay());
        if (dto.monsoonEndMonthDay() != null) cfg.setMonsoonEndMonthDay(dto.monsoonEndMonthDay());
        cfg.setDistrictCode(dto.districtCode());
        ProjectScheduleConfig saved = configRepo.save(cfg);
        holidayService.evictProject(projectId);
        return toDto(saved);
    }

    @Transactional
    public ProjectHolidayOverride addOverride(Long projectId, HolidayOverrideRequest req) {
        ProjectHolidayOverride o = new ProjectHolidayOverride();
        o.setProjectId(projectId);
        o.setHolidayId(req.holidayId());
        o.setOverrideDate(req.overrideDate());
        o.setOverrideName(req.overrideName());
        o.setAction(req.action());
        ProjectHolidayOverride saved = overrideRepo.save(o);
        holidayService.evictProject(projectId);
        return saved;
    }

    @Transactional
    public void deleteOverride(Long projectId, Long overrideId) {
        ProjectHolidayOverride o = overrideRepo.findById(overrideId)
                .orElseThrow(() -> new IllegalArgumentException("Override not found: " + overrideId));
        if (!o.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Override " + overrideId + " does not belong to project " + projectId);
        }
        overrideRepo.delete(o);
        holidayService.evictProject(projectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectHolidayOverride> listOverrides(Long projectId) {
        return overrideRepo.findByProjectId(projectId);
    }

    private ProjectScheduleConfigDto toDto(ProjectScheduleConfig c) {
        return new ProjectScheduleConfigDto(
                c.getProjectId(),
                c.getSundayWorking(),
                c.getMonsoonStartMonthDay(),
                c.getMonsoonEndMonthDay(),
                c.getDistrictCode());
    }
}
