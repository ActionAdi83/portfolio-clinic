package edu.clinic.config;

import edu.clinic.entities.Appointment;
import edu.clinic.entities.WeeklyScheduleDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Creates the uniqueness constraints this service relies on.
 *
 * Spring Boot leaves {@code spring.data.mongodb.auto-index-creation} off, so these
 * are declared explicitly at startup rather than assumed from an {@code @Indexed}
 * annotation — see fanvote-backend's PaymentIndexInitializer, which this mirrors.
 *
 * The appointment index is a plain compound index on (date, startTime), not unique.
 * A unique index excluding cancelled appointments needs a MongoDB partial index,
 * and partialFilterExpression only supports equality, $exists, $gt/$gte/$lt/$lte,
 * $type and a top-level $and of those — {@code status != CANCELLED} needs $ne,
 * which MongoDB rejects at index-creation time ("Expression not supported in
 * partial index: $not"). Expressing this properly means adding a field like
 * {@code cancelledAt} that only exists once cancelled and filtering on
 * {@code $exists: false} — a real entity change, not just an index tweak.
 * Until that lands, double-booking protection is the service-level
 * check-then-insert in AppointmentServiceImpl, which is not atomic under true
 * concurrent requests — an acceptable gap for this project's realistic traffic,
 * but worth revisiting before this pattern is copied somewhere with real load.
 */
@Configuration
public class IndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(IndexInitializer.class);

    @Bean
    public ApplicationRunner clinicIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            try {
                mongoTemplate.indexOps(Appointment.class).ensureIndex(new Index()
                        .on("date", Sort.Direction.ASC)
                        .on("startTime", Sort.Direction.ASC));

                // Only seven of these ever exist (one per weekday); this guards against
                // ScheduleSeedInitializer or a future admin write ever accidentally
                // creating a second document for the same day.
                mongoTemplate.indexOps(WeeklyScheduleDay.class)
                        .ensureIndex(new Index().on("dayOfWeek", Sort.Direction.ASC).unique());
            } catch (RuntimeException e) {
                // Worth shouting about: without the appointment index, two requests that
                // both pass the in-memory availability check can both insert and
                // double-book a slot.
                log.error("Could not create the clinic uniqueness indexes", e);
            }
        };
    }
}
