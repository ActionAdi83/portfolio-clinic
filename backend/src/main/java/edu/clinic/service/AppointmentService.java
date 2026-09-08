package edu.clinic.service;

import edu.clinic.dto.AppointmentDTO;
import edu.clinic.dto.AppointmentStatusUpdateRequest;
import edu.clinic.dto.BookAppointmentRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentService {

    /** Free slot start times for a given date and service, in chronological order. */
    List<LocalTime> availableSlots(LocalDate date, String serviceId);

    AppointmentDTO book(String userId, BookAppointmentRequest request);

    List<AppointmentDTO> ownAppointments(String userId);

    void cancelOwn(String userId, String appointmentId);

    List<AppointmentDTO> listAll();

    AppointmentDTO updateStatus(String appointmentId, AppointmentStatusUpdateRequest request);
}
