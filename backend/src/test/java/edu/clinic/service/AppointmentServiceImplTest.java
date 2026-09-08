package edu.clinic.service;

import edu.clinic.dto.BookAppointmentRequest;
import edu.clinic.entities.Appointment;
import edu.clinic.entities.AppointmentStatus;
import edu.clinic.entities.MedicalService;
import edu.clinic.entities.WeeklyScheduleDay;
import edu.clinic.exceptions.SlotUnavailableException;
import edu.clinic.repository.AppointmentRepository;
import edu.clinic.repository.MedicalServiceRepository;
import edu.clinic.repository.WeeklyScheduleDayRepository;
import edu.clinic.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Deliberately not a {@code @SpringBootTest}: this project's CommandLineRunner
 * beans (schedule seeding, index creation) talk to a real MongoDB at context
 * startup, which a plain unit test run should not require. This exercises the
 * slot-computation algorithm directly against mocked repositories instead.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private WeeklyScheduleDayRepository scheduleRepository;
    @Mock
    private MedicalServiceRepository serviceRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private WeeklyScheduleDay mondayNineToFiveEveryThirty() {
        WeeklyScheduleDay day = new WeeklyScheduleDay();
        day.setDayOfWeek(DayOfWeek.MONDAY);
        day.setActive(true);
        day.setStartTime(LocalTime.of(9, 0));
        day.setEndTime(LocalTime.of(17, 0));
        day.setSlotDurationMinutes(30);
        return day;
    }

    private MedicalService thirtyMinuteService() {
        MedicalService service = new MedicalService();
        service.setId("svc-1");
        service.setTitle("Consult");
        service.setDurationMinutes(30);
        service.setActive(true);
        return service;
    }

    @Test
    void closedDayHasNoSlots() {
        LocalDate closedMonday = LocalDate.of(2026, 9, 7); // a Monday
        MedicalService service = thirtyMinuteService();
        WeeklyScheduleDay closed = mondayNineToFiveEveryThirty();
        closed.setActive(false);

        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(service));
        when(scheduleRepository.findByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(Optional.of(closed));

        assertTrue(appointmentService.availableSlots(closedMonday, "svc-1").isEmpty());
    }

    @Test
    void generatesEveryHalfHourSlotWhenNothingIsBooked() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(thirtyMinuteService()));
        when(scheduleRepository.findByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(Optional.of(mondayNineToFiveEveryThirty()));
        when(appointmentRepository.findByDateAndStatusNot(monday, AppointmentStatus.CANCELLED))
                .thenReturn(List.of());

        List<LocalTime> slots = appointmentService.availableSlots(monday, "svc-1");

        assertEquals(16, slots.size()); // 09:00 .. 16:30 every 30 minutes
        assertEquals(LocalTime.of(9, 0), slots.get(0));
        assertEquals(LocalTime.of(16, 30), slots.get(slots.size() - 1));
    }

    @Test
    void excludesASlotThatOverlapsAnExistingAppointment() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Appointment existing = new Appointment();
        existing.setDate(monday);
        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(10, 30));
        existing.setStatus(AppointmentStatus.CONFIRMED);

        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(thirtyMinuteService()));
        when(scheduleRepository.findByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(Optional.of(mondayNineToFiveEveryThirty()));
        when(appointmentRepository.findByDateAndStatusNot(monday, AppointmentStatus.CANCELLED))
                .thenReturn(List.of(existing));

        List<LocalTime> slots = appointmentService.availableSlots(monday, "svc-1");

        assertFalse(slots.contains(LocalTime.of(10, 0)));
        assertTrue(slots.contains(LocalTime.of(9, 30)));
        assertTrue(slots.contains(LocalTime.of(10, 30)));
    }

    @Test
    void bookingARequestedButNoLongerFreeSlotIsRejected() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Appointment existing = new Appointment();
        existing.setDate(monday);
        existing.setStartTime(LocalTime.of(9, 0));
        existing.setEndTime(LocalTime.of(9, 30));
        existing.setStatus(AppointmentStatus.CONFIRMED);

        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(thirtyMinuteService()));
        when(scheduleRepository.findByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(Optional.of(mondayNineToFiveEveryThirty()));
        when(appointmentRepository.findByDateAndStatusNot(monday, AppointmentStatus.CANCELLED))
                .thenReturn(List.of(existing));

        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setServiceId("svc-1");
        request.setDate(monday);
        request.setStartTime(LocalTime.of(9, 0)); // already taken
        request.setPatientName("Ana Pop");
        request.setPatientContact("ana@example.com");

        assertThrows(SlotUnavailableException.class, () -> appointmentService.book("user-1", request));
    }
}
