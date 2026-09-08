package edu.clinic.dto;

import edu.clinic.entities.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentStatusUpdateRequest {
    @NotNull
    private AppointmentStatus status;
}
