package edu.clinic.service.impl;

import edu.clinic.dto.ScheduleDayDTO;
import edu.clinic.entities.WeeklyScheduleDay;
import edu.clinic.exceptions.NotFoundException;
import edu.clinic.repository.WeeklyScheduleDayRepository;
import edu.clinic.service.ScheduleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final WeeklyScheduleDayRepository repository;

    public ScheduleServiceImpl(WeeklyScheduleDayRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ScheduleDayDTO> getWeeklySchedule() {
        return repository.findAllByOrderByDayOfWeekAsc().stream().map(this::toDto).toList();
    }

    /**
     * Updates each of the 7 days by weekday, in place. Days are matched by
     * {@code dayOfWeek} rather than by document id, since the admin editor never
     * needs to know Mongo ids — it works with "MONDAY", "TUESDAY", ....
     */
    @Override
    public List<ScheduleDayDTO> updateWeeklySchedule(List<ScheduleDayDTO> days) {
        for (ScheduleDayDTO dto : days) {
            WeeklyScheduleDay entity = repository.findByDayOfWeek(dto.getDayOfWeek())
                    .orElseThrow(() -> new NotFoundException("No schedule document for " + dto.getDayOfWeek()));
            entity.setActive(dto.isActive());
            entity.setStartTime(dto.getStartTime());
            entity.setEndTime(dto.getEndTime());
            entity.setSlotDurationMinutes(dto.getSlotDurationMinutes());
            repository.save(entity);
        }
        return getWeeklySchedule();
    }

    private ScheduleDayDTO toDto(WeeklyScheduleDay entity) {
        ScheduleDayDTO dto = new ScheduleDayDTO();
        dto.setDayOfWeek(entity.getDayOfWeek());
        dto.setActive(entity.isActive());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setSlotDurationMinutes(entity.getSlotDurationMinutes());
        return dto;
    }
}
