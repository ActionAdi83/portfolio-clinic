package edu.clinic.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A bookable clinic service (consultation, procedure, ...).
 *
 * Named MedicalService rather than "Service" on purpose — a domain class called
 * Service in a Spring codebase reads exactly like a {@code @Service} bean and
 * invites the kind of confusion that costs someone twenty minutes during a code
 * review.
 */
@Getter
@Setter
@Document(collection = "services")
public class MedicalService {

    @Id
    private String id;

    private String title;
    private String description;

    /** Left empty or pointed at a placeholder image; no real photos are seeded. */
    private String imageUrl;

    private Integer durationMinutes;

    /** So admin can hide a service without deleting its booking history. */
    private boolean active = true;
}
