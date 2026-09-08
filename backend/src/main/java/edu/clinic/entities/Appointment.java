package edu.clinic.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;

    /** Keycloak "sub" of the patient who booked this. Never taken from a request body. */
    private String userId;

    // Snapshotted at booking time rather than joined from the account at read time —
    // a patient's display name changing later should not rewrite past appointments,
    // and the admin appointment list needs a name/contact without a second lookup.
    private String patientName;
    private String patientContact;

    private String serviceId;
    /** Snapshot of the service title at booking time, for the same reason as above. */
    private String serviceTitle;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    private Instant createdAt = Instant.now();
}
