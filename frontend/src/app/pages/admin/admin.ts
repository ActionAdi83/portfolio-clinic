import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import Keycloak from 'keycloak-js';
import { MedicalServiceApi } from '../../services/medical-service.service';
import { ScheduleApi } from '../../services/schedule.service';
import { AppointmentApi } from '../../services/appointment.service';
import { MedicalServiceDTO } from '../../entities/medical-service';
import { DAYS_OF_WEEK, DAY_LABELS, ScheduleDayDTO } from '../../entities/schedule-day';
import { AppointmentDTO, AppointmentStatus } from '../../entities/appointment';

type AdminTab = 'services' | 'schedule' | 'appointments';

const EMPTY_SERVICE: MedicalServiceDTO = {
  title: '',
  description: '',
  imageUrl: '',
  durationMinutes: 30,
  active: true,
};

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class AdminPage implements OnInit {
  private readonly serviceApi = inject(MedicalServiceApi);
  private readonly scheduleApi = inject(ScheduleApi);
  private readonly appointmentApi = inject(AppointmentApi);
  private readonly keycloak = inject(Keycloak);

  /** Same claim the backend enforces on — non-admins get a read-only view. */
  get isAdmin(): boolean {
    const roles: string[] = (this.keycloak?.tokenParsed as any)?.realm_access?.roles ?? [];
    return roles.includes('clinic-admin');
  }

  readonly dayOrder = DAYS_OF_WEEK;
  readonly dayLabels = DAY_LABELS;
  readonly statuses: AppointmentStatus[] = ['REQUESTED', 'CONFIRMED', 'CANCELLED', 'COMPLETED'];

  tab = signal<AdminTab>('services');

  // --- Services ---------------------------------------------------------
  readonly services = signal<MedicalServiceDTO[]>([]);
  servicesLoading = signal(true);
  servicesError = signal<string | null>(null);
  editingService: MedicalServiceDTO = { ...EMPTY_SERVICE };
  savingService = signal(false);

  // --- Schedule -----------------------------------------------------------
  readonly schedule = signal<ScheduleDayDTO[]>([]);
  scheduleLoading = signal(true);
  scheduleError = signal<string | null>(null);
  savingSchedule = signal(false);
  scheduleSaved = signal(false);

  // --- Appointments -------------------------------------------------------
  readonly appointments = signal<AppointmentDTO[]>([]);
  appointmentsLoading = signal(true);
  appointmentsError = signal<string | null>(null);
  updatingAppointmentId = signal<string | null>(null);

  ngOnInit(): void {
    this.loadServices();
    this.loadSchedule();
    this.loadAppointments();
  }

  setTab(tab: AdminTab): void {
    this.tab.set(tab);
  }

  // --- Services -----------------------------------------------------------

  loadServices(): void {
    this.servicesLoading.set(true);
    this.serviceApi.listAll().subscribe({
      next: (services) => {
        this.services.set(services);
        this.servicesLoading.set(false);
      },
      error: () => {
        this.servicesError.set('Nu am putut încărca serviciile.');
        this.servicesLoading.set(false);
      },
    });
  }

  editService(service: MedicalServiceDTO): void {
    this.editingService = { ...service };
  }

  newService(): void {
    this.editingService = { ...EMPTY_SERVICE };
  }

  saveService(): void {
    this.savingService.set(true);
    this.servicesError.set(null);
    const dto = this.editingService;
    const request = dto.id
      ? this.serviceApi.update(dto.id, dto)
      : this.serviceApi.create(dto);
    request.subscribe({
      next: () => {
        this.savingService.set(false);
        this.newService();
        this.loadServices();
      },
      error: () => {
        this.savingService.set(false);
        this.servicesError.set('Serviciul nu a putut fi salvat.');
      },
    });
  }

  toggleActive(service: MedicalServiceDTO): void {
    if (!service.id) return;
    this.serviceApi.update(service.id, { ...service, active: !service.active }).subscribe({
      next: () => this.loadServices(),
      error: () => this.servicesError.set('Nu am putut actualiza starea serviciului.'),
    });
  }

  deleteService(service: MedicalServiceDTO): void {
    if (!service.id) return;
    if (!confirm(`Ștergi serviciul "${service.title}"?`)) return;
    this.serviceApi.delete(service.id).subscribe({
      next: () => this.loadServices(),
      error: () => this.servicesError.set('Serviciul nu a putut fi șters.'),
    });
  }

  // --- Schedule -------------------------------------------------------------

  loadSchedule(): void {
    this.scheduleLoading.set(true);
    this.scheduleApi.get().subscribe({
      next: (days) => {
        // Sorted server-side, but re-sort defensively into a fixed Mon-Sun order for display.
        const byDay = new Map(days.map(d => [d.dayOfWeek, d]));
        this.schedule.set(this.dayOrder.map(d => byDay.get(d)!).filter(Boolean));
        this.scheduleLoading.set(false);
      },
      error: () => {
        this.scheduleError.set('Nu am putut încărca programul săptămânal.');
        this.scheduleLoading.set(false);
      },
    });
  }

  saveSchedule(): void {
    this.savingSchedule.set(true);
    this.scheduleError.set(null);
    this.scheduleSaved.set(false);
    this.scheduleApi.update(this.schedule()).subscribe({
      next: (days) => {
        this.schedule.set(days);
        this.savingSchedule.set(false);
        this.scheduleSaved.set(true);
      },
      error: () => {
        this.savingSchedule.set(false);
        this.scheduleError.set('Programul nu a putut fi salvat.');
      },
    });
  }

  // --- Appointments -----------------------------------------------------------

  loadAppointments(): void {
    this.appointmentsLoading.set(true);
    this.appointmentApi.listAll().subscribe({
      next: (appointments) => {
        this.appointments.set(appointments);
        this.appointmentsLoading.set(false);
      },
      error: () => {
        this.appointmentsError.set('Nu am putut încărca programările.');
        this.appointmentsLoading.set(false);
      },
    });
  }

  setStatus(appointment: AppointmentDTO, status: AppointmentStatus): void {
    this.updatingAppointmentId.set(appointment.id);
    this.appointmentApi.updateStatus(appointment.id, status).subscribe({
      next: () => {
        this.updatingAppointmentId.set(null);
        this.loadAppointments();
      },
      error: () => {
        this.updatingAppointmentId.set(null);
        this.appointmentsError.set('Starea programării nu a putut fi actualizată.');
      },
    });
  }
}
