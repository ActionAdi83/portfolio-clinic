package edu.clinic.controllers;

import edu.clinic.dto.MedicalServiceDTO;
import edu.clinic.service.MedicalServiceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Service CRUD. Access is enforced centrally in SecurityConfig
 * ({@code /api/admin/**} requires realm role {@code clinic-admin}) — no
 * per-method @PreAuthorize needed here, but see WeeklyScheduleDay's controller
 * sibling for the same reasoning if this ever moves.
 */
@RestController
@RequestMapping("/api/admin/services")
public class AdminServiceController {

    private final MedicalServiceService service;

    public AdminServiceController(MedicalServiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<MedicalServiceDTO> listAll() {
        return service.listAll();
    }

    @PostMapping
    public ResponseEntity<MedicalServiceDTO> create(@Valid @RequestBody MedicalServiceDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalServiceDTO> update(@PathVariable String id, @Valid @RequestBody MedicalServiceDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
