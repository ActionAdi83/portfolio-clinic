package edu.clinic.service.impl;

import edu.clinic.dto.MedicalServiceDTO;
import edu.clinic.entities.MedicalService;
import edu.clinic.exceptions.NotFoundException;
import edu.clinic.repository.MedicalServiceRepository;
import edu.clinic.service.MedicalServiceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalServiceServiceImpl implements MedicalServiceService {

    private final MedicalServiceRepository repository;

    public MedicalServiceServiceImpl(MedicalServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MedicalServiceDTO> listActive() {
        return repository.findByActiveTrue().stream().map(this::toDto).toList();
    }

    @Override
    public List<MedicalServiceDTO> listAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public MedicalServiceDTO create(MedicalServiceDTO dto) {
        MedicalService entity = new MedicalService();
        applyDto(entity, dto);
        return toDto(repository.save(entity));
    }

    @Override
    public MedicalServiceDTO update(String id, MedicalServiceDTO dto) {
        MedicalService entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No such service: " + id));
        applyDto(entity, dto);
        return toDto(repository.save(entity));
    }

    @Override
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("No such service: " + id);
        }
        repository.deleteById(id);
    }

    private void applyDto(MedicalService entity, MedicalServiceDTO dto) {
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setImageUrl(dto.getImageUrl());
        entity.setDurationMinutes(dto.getDurationMinutes());
        entity.setActive(dto.isActive());
    }

    private MedicalServiceDTO toDto(MedicalService entity) {
        MedicalServiceDTO dto = new MedicalServiceDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        dto.setDurationMinutes(entity.getDurationMinutes());
        dto.setActive(entity.isActive());
        return dto;
    }
}
