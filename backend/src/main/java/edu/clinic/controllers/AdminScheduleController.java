package edu.clinic.controllers;

import edu.clinic.dto.ScheduleDayDTO;
import edu.clinic.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/schedule")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    public AdminScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ScheduleDayDTO> get() {
        return scheduleService.getWeeklySchedule();
    }

    /** Replaces the 7 days wholesale — the admin editor always submits all of them. */
    @PutMapping
    public List<ScheduleDayDTO> update(@RequestBody List<ScheduleDayDTO> days) {
        return scheduleService.updateWeeklySchedule(days);
    }
}
