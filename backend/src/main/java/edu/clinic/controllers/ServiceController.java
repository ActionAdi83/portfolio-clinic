package edu.clinic.controllers;

import edu.clinic.dto.MedicalServiceDTO;
import edu.clinic.service.MedicalServiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public catalogue — anyone deciding whether to book can read this without an account. */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final MedicalServiceService service;

    public ServiceController(MedicalServiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<MedicalServiceDTO> listActive() {
        return service.listActive();
    }
}
