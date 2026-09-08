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
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * Creates the uniqueness constraints this service relies on.
 *
 * Spring Boot leaves {@code spring.data.mongodb.auto-index-creation} off, so these
 * are declared explicitly at startup rather than assumed from an {@code @Indexed}
 * annotation — see fanvote-backend's PaymentIndexInitializer, which this mirrors.
 *
 * The appointment index is partial (only non-cancelled appointments) rather than a
 * plain unique index on (date, startTime): a plain unique index would forbid ever
 * booking a slot again after the appointment that first held it was cancelled.
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
                        .on("startTime", Sort.Direction.ASC)
                        .unique()
                        .partial(PartialIndexFilter.of(Criteria.where("status").ne("CANCELLED"))));

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
