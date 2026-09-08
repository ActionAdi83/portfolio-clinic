package edu.clinic.service;

import edu.clinic.dto.MedicalServiceDTO;

import java.util.List;

public interface MedicalServiceService {
    List<MedicalServiceDTO> listActive();

    List<MedicalServiceDTO> listAll();

    MedicalServiceDTO create(MedicalServiceDTO dto);

    MedicalServiceDTO update(String id, MedicalServiceDTO dto);

    void delete(String id);
}
