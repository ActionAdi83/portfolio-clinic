package edu.clinic.service;

import edu.clinic.dto.ScheduleDayDTO;

import java.util.List;

public interface ScheduleService {
    List<ScheduleDayDTO> getWeeklySchedule();

    List<ScheduleDayDTO> updateWeeklySchedule(List<ScheduleDayDTO> days);
}
