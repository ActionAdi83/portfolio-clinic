package edu.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class BookAppointmentRequest {

    @NotBlank
    private String serviceId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime startTime;

    @NotBlank
    private String patientName;

    /** Email or phone; free text, snapshotted onto the appointment. */
    @NotBlank
    private String patientContact;
}
