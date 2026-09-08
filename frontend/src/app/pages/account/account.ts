import { Component, OnInit, inject, signal } from '@angular/core';
import { AppointmentApi } from '../../services/appointment.service';
import { AppointmentDTO } from '../../entities/appointment';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [],
  templateUrl: './account.html',
  styleUrl: './account.css',
})
export class AccountPage implements OnInit {
  private readonly api = inject(AppointmentApi);

  readonly appointments = signal<AppointmentDTO[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly cancellingId = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.api.ownAppointments().subscribe({
      next: (appointments) => {
        this.appointments.set(appointments);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Nu am putut încărca programările tale.');
        this.loading.set(false);
      },
    });
  }

  canCancel(appointment: AppointmentDTO): boolean {
    return appointment.status === 'REQUESTED' || appointment.status === 'CONFIRMED';
  }

  cancel(appointment: AppointmentDTO): void {
    this.cancellingId.set(appointment.id);
    this.api.cancelOwn(appointment.id).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.load();
      },
      error: () => {
        this.cancellingId.set(null);
        this.error.set('Anularea nu a putut fi efectuată. Încearcă din nou.');
      },
    });
  }
}
