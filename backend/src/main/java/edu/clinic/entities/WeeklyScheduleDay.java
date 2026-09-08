package edu.clinic.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * One document per weekday, edited by the admin to set "program pe zile ale
 * săptămânii, la ce interval se iau programări" — is the clinic open that day,
 * between which hours, and how far apart are the bookable slots.
 *
 * Exactly seven of these ever exist; {@code ScheduleSeedInitializer} creates the
 * default set on first startup if the collection is empty.
 */
@Getter
@Setter
@Document(collection = "weekly_schedule")
public class WeeklyScheduleDay {

    @Id
    private String id;

    @Indexed(unique = true)
    private DayOfWeek dayOfWeek;

    /** Is the clinic open at all this weekday. */
    private boolean active;

    private LocalTime startTime;
    private LocalTime endTime;

    /** Granularity of bookable slots, e.g. 30 for every half hour. */
    private Integer slotDurationMinutes;
}
