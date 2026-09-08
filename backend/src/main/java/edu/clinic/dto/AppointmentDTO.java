package edu.clinic.dto;

import edu.clinic.entities.Appointment;
import edu.clinic.entities.AppointmentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class AppointmentDTO {
    private String id;
    private String serviceId;
    private String serviceTitle;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private AppointmentStatus status;
    private Instant createdAt;

    // Only populated for the admin listing — the account endpoint returns the
    // caller's own appointments, so there is no need to echo their own name back,
    // and the public available-slots endpoint never returns appointment objects
    // at all, only free/busy times.
    private String patientName;
    private String patientContact;

    public static AppointmentDTO of(Appointment a, boolean includePatientDetails) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(a.getId());
        dto.setServiceId(a.getServiceId());
        dto.setServiceTitle(a.getServiceTitle());
        dto.setDate(a.getDate());
        dto.setStartTime(a.getStartTime());
        dto.setEndTime(a.getEndTime());
        dto.setStatus(a.getStatus());
        dto.setCreatedAt(a.getCreatedAt());
        if (includePatientDetails) {
            dto.setPatientName(a.getPatientName());
            dto.setPatientContact(a.getPatientContact());
        }
        return dto;
    }
}
