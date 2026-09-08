package edu.clinic.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class ScheduleDayDTO {
    private DayOfWeek dayOfWeek;
    private boolean active;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
}
