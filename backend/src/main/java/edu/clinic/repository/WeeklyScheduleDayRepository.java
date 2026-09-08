package edu.clinic.repository;

import edu.clinic.entities.WeeklyScheduleDay;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyScheduleDayRepository extends MongoRepository<WeeklyScheduleDay, String> {
    Optional<WeeklyScheduleDay> findByDayOfWeek(DayOfWeek dayOfWeek);

    List<WeeklyScheduleDay> findAllByOrderByDayOfWeekAsc();
}
