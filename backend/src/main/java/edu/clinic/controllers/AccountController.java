package edu.clinic.controllers;

import edu.clinic.dto.AppointmentDTO;
import edu.clinic.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The signed-in patient's own appointment history — identity always comes from the token. */
@RestController
@RequestMapping("/api/account/appointments")
public class AccountController {

    private final AppointmentService appointmentService;

    public AccountController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<AppointmentDTO> own(@AuthenticationPrincipal Jwt jwt) {
        return appointmentService.ownAppointments(jwt.getSubject());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        appointmentService.cancelOwn(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
