package edu.clinic.service.impl;

import edu.clinic.dto.AppointmentDTO;
import edu.clinic.dto.AppointmentStatusUpdateRequest;
import edu.clinic.dto.BookAppointmentRequest;
import edu.clinic.entities.Appointment;
import edu.clinic.entities.AppointmentStatus;
import edu.clinic.entities.MedicalService;
import edu.clinic.entities.WeeklyScheduleDay;
import edu.clinic.exceptions.NotFoundException;
import edu.clinic.exceptions.SlotUnavailableException;
import edu.clinic.repository.AppointmentRepository;
import edu.clinic.repository.MedicalServiceRepository;
import edu.clinic.repository.WeeklyScheduleDayRepository;
import edu.clinic.service.AppointmentService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final WeeklyScheduleDayRepository scheduleRepository;
    private final MedicalServiceRepository serviceRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                   WeeklyScheduleDayRepository scheduleRepository,
                                   MedicalServiceRepository serviceRepository) {
        this.appointmentRepository = appointmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public List<LocalTime> availableSlots(LocalDate date, String serviceId) {
        MedicalService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("No such service: " + serviceId));
        WeeklyScheduleDay day = scheduleRepository.findByDayOfWeek(date.getDayOfWeek()).orElse(null);
        if (day == null || !day.isActive()) {
            return List.of();
        }
        return computeFreeSlots(date, day, service.getDurationMinutes(), null);
    }

    /**
     * Candidate slots step by the day's configured granularity (e.g. every 30
     * minutes), but each candidate occupies the *service's* duration, not the
     * granularity itself — a 60 minute service starting at 09:00 blocks 09:00-10:00
     * even though slots are offered every half hour. A slot is excluded if that
     * span overlaps any existing non-cancelled appointment, or if it would run
     * past the day's closing time.
     *
     * @param ignoreAppointmentId excluded from the busy set — used when re-validating
     *                            a booking so the appointment being created doesn't
     *                            collide with itself (id is null before insert; kept
     *                            as a parameter for symmetry / future reuse).
     */
    private List<LocalTime> computeFreeSlots(LocalDate date, WeeklyScheduleDay day, int serviceDurationMinutes,
                                              String ignoreAppointmentId) {
        List<Appointment> existing = appointmentRepository
                .findByDateAndStatusNot(date, AppointmentStatus.CANCELLED)
                .stream()
                .filter(a -> ignoreAppointmentId == null || !ignoreAppointmentId.equals(a.getId()))
                .toList();

        List<LocalTime> free = new ArrayList<>();
        LocalTime candidate = day.getStartTime();
        int step = day.getSlotDurationMinutes();
        while (true) {
            LocalTime candidateEnd = candidate.plusMinutes(serviceDurationMinutes);
            // isAfter is false when candidateEnd == endTime exactly, which is the
            // intended "last slot ends exactly at closing time" boundary.
            if (candidateEnd.isAfter(day.getEndTime()) || candidateEnd.isBefore(candidate)) {
                break; // past closing, or wrapped past midnight
            }
            LocalTime slotStart = candidate;
            boolean overlaps = existing.stream().anyMatch(a ->
                    slotStart.isBefore(a.getEndTime()) && a.getStartTime().isBefore(candidateEnd));
            if (!overlaps) {
                free.add(slotStart);
            }
            LocalTime next = candidate.plusMinutes(step);
            if (!next.isAfter(candidate)) {
                break; // guards against a zero/negative slotDurationMinutes misconfiguration
            }
            candidate = next;
        }
        return free;
    }

    @Override
    public AppointmentDTO book(String userId, BookAppointmentRequest request) {
        MedicalService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new NotFoundException("No such service: " + request.getServiceId()));
        WeeklyScheduleDay day = scheduleRepository.findByDayOfWeek(request.getDate().getDayOfWeek())
                .orElseThrow(() -> new SlotUnavailableException("The clinic has no schedule for that day"));
        if (!day.isActive()) {
            throw new SlotUnavailableException("The clinic is closed that day");
        }

        LocalTime start = request.getStartTime();
        LocalTime end = start.plusMinutes(service.getDurationMinutes());

        // Re-validate server-side that the requested start time is one of the
        // currently free slots — the browser's list may be stale by the time the
        // booking request arrives.
        List<LocalTime> free = computeFreeSlots(request.getDate(), day, service.getDurationMinutes(), null);
        if (!free.contains(start)) {
            throw new SlotUnavailableException("That slot is no longer available");
        }

        Appointment appointment = new Appointment();
        appointment.setUserId(userId);
        appointment.setPatientName(request.getPatientName());
        appointment.setPatientContact(request.getPatientContact());
        appointment.setServiceId(service.getId());
        appointment.setServiceTitle(service.getTitle());
        appointment.setDate(request.getDate());
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setStatus(AppointmentStatus.REQUESTED);

        try {
            // The read-then-write above is a check, not a guarantee — two requests
            // can both pass it before either inserts. AppointmentIndexInitializer
            // creates a unique partial index on (date, startTime) for non-cancelled
            // appointments, so the loser of that race fails here with a
            // DuplicateKeyException instead of silently double-booking the slot.
            return AppointmentDTO.of(appointmentRepository.save(appointment), true);
        } catch (DuplicateKeyException e) {
            throw new SlotUnavailableException("That slot was just taken — please pick another one");
        }
    }

    @Override
    public List<AppointmentDTO> ownAppointments(String userId) {
        return appointmentRepository.findByUserIdOrderByDateDescStartTimeDesc(userId).stream()
                .map(a -> AppointmentDTO.of(a, false))
                .toList();
    }

    @Override
    public void cancelOwn(String userId, String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("No such appointment: " + appointmentId));
        if (!appointment.getUserId().equals(userId)) {
            // Deliberately the same NotFoundException as a missing id, not 403 — this
            // avoids confirming to a caller that a given id belongs to someone else.
            throw new NotFoundException("No such appointment: " + appointmentId);
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Override
    public List<AppointmentDTO> listAll() {
        return appointmentRepository.findAllByOrderByDateDescStartTimeDesc().stream()
                .map(a -> AppointmentDTO.of(a, true))
                .toList();
    }

    @Override
    public AppointmentDTO updateStatus(String appointmentId, AppointmentStatusUpdateRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("No such appointment: " + appointmentId));
        appointment.setStatus(request.getStatus());
        return AppointmentDTO.of(appointmentRepository.save(appointment), true);
    }
}
