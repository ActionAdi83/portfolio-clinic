package edu.clinic.repository;

import edu.clinic.entities.MedicalService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalServiceRepository extends MongoRepository<MedicalService, String> {
    List<MedicalService> findByActiveTrue();
}
