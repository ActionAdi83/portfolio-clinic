package edu.clinic.controllers;

import edu.clinic.dto.AppointmentDTO;
import edu.clinic.dto.AppointmentStatusUpdateRequest;
import edu.clinic.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    public AdminAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<AppointmentDTO> listAll() {
        return appointmentService.listAll();
    }

    @PutMapping("/{id}/status")
    public AppointmentDTO updateStatus(@PathVariable String id, @Valid @RequestBody AppointmentStatusUpdateRequest request) {
        return appointmentService.updateStatus(id, request);
    }
}
