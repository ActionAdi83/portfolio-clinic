package edu.clinic.controllers;

import edu.clinic.dto.AppointmentDTO;
import edu.clinic.dto.BookAppointmentRequest;
import edu.clinic.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Public: reveals only which start times are free for a given date/service,
     * never who holds the busy ones — no patient names or ids in the response.
     */
    @GetMapping("/available-slots")
    public List<LocalTime> availableSlots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                           @RequestParam String serviceId) {
        return appointmentService.availableSlots(date, serviceId);
    }

    @PostMapping
    public AppointmentDTO book(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BookAppointmentRequest request) {
        return appointmentService.book(jwt.getSubject(), request);
    }
}
