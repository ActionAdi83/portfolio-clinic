package edu.clinic.config;

import edu.clinic.entities.WeeklyScheduleDay;
import edu.clinic.repository.WeeklyScheduleDayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;

/**
 * Seeds the 7 {@link WeeklyScheduleDay} documents on first startup, if the
 * collection is empty. Idempotent — checked with {@code count() == 0} rather than
 * gated behind a destructive/opt-in flag like fanvote's demo-data seeder, because
 * this never overwrites an admin's edits: it only fills in what is missing.
 *
 * Default: Monday-Friday 09:00-17:00, 30 minute slots, active; Saturday/Sunday
 * inactive (closed).
 */
@Component
public class ScheduleSeedInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduleSeedInitializer.class);
    private static final EnumSet<DayOfWeek> WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final WeeklyScheduleDayRepository repository;

    public ScheduleSeedInitializer(WeeklyScheduleDayRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        log.info("[schedule-seed] weekly_schedule is empty — seeding the default Mon-Fri 09:00-17:00 schedule");
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            WeeklyScheduleDay day = new WeeklyScheduleDay();
            day.setDayOfWeek(dayOfWeek);
            boolean weekend = WEEKEND.contains(dayOfWeek);
            day.setActive(!weekend);
            day.setStartTime(LocalTime.of(9, 0));
            day.setEndTime(LocalTime.of(17, 0));
            day.setSlotDurationMinutes(30);
            repository.save(day);
        }
    }
}
