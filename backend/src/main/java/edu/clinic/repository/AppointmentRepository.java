package edu.clinic.repository;

import edu.clinic.entities.Appointment;
import edu.clinic.entities.AppointmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    List<Appointment> findByDateAndStatusNot(LocalDate date, AppointmentStatus excludedStatus);

    List<Appointment> findByUserIdOrderByDateDescStartTimeDesc(String userId);

    List<Appointment> findAllByOrderByDateDescStartTimeDesc();
}
