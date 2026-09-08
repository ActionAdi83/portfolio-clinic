import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import Keycloak from 'keycloak-js';
import { MedicalServiceApi } from '../../services/medical-service.service';
import { AppointmentApi } from '../../services/appointment.service';
import { MedicalServiceDTO } from '../../entities/medical-service';

@Component({
  selector: 'app-book',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './book.html',
  styleUrl: './book.css',
})
export class BookPage implements OnInit {
  private readonly serviceApi = inject(MedicalServiceApi);
  private readonly appointmentApi = inject(AppointmentApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly keycloak = inject(Keycloak);

  readonly services = signal<MedicalServiceDTO[]>([]);
  readonly slots = signal<string[]>([]);
  readonly loadingServices = signal(true);
  readonly loadingSlots = signal(false);
  readonly booking = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  selectedServiceId = '';
  selectedDate = this.todayIso();
  selectedSlot: string | null = null;
  patientName = '';
  patientContact = '';

  // available-slots is queried publicly (no auth) and only booking itself is
  // guarded — see AppointmentController — but this page is behind the auth
  // guard anyway, so the name/contact fields can be prefilled from the token.
  ngOnInit(): void {
    const claims = (this.keycloak.tokenParsed ?? {}) as any;
    this.patientName = claims.name ?? '';
    this.patientContact = claims.email ?? '';

    this.serviceApi.listActive().subscribe({
      next: (services) => {
        this.services.set(services);
        this.loadingServices.set(false);
        const preselected = this.route.snapshot.queryParamMap.get('serviceId');
        if (preselected && services.some(s => s.id === preselected)) {
          this.selectedServiceId = preselected;
        } else if (services.length > 0) {
          this.selectedServiceId = services[0].id!;
        }
        this.refreshSlots();
      },
      error: () => {
        this.error.set('Nu am putut încărca lista de servicii.');
        this.loadingServices.set(false);
      },
    });
  }

  onServiceOrDateChange(): void {
    this.selectedSlot = null;
    this.refreshSlots();
  }

  refreshSlots(): void {
    if (!this.selectedServiceId || !this.selectedDate) {
      return;
    }
    this.loadingSlots.set(true);
    this.slots.set([]);
    this.appointmentApi.availableSlots(this.selectedDate, this.selectedServiceId).subscribe({
      next: (slots) => {
        this.slots.set(slots);
        this.loadingSlots.set(false);
      },
      error: () => {
        this.error.set('Nu am putut încărca sloturile disponibile pentru această zi.');
        this.loadingSlots.set(false);
      },
    });
  }

  selectSlot(slot: string): void {
    this.selectedSlot = slot;
    this.success.set(null);
  }

  confirmBooking(): void {
    if (!this.selectedSlot || !this.selectedServiceId || !this.patientName || !this.patientContact) {
      return;
    }
    this.booking.set(true);
    this.error.set(null);
    this.appointmentApi.book({
      serviceId: this.selectedServiceId,
      date: this.selectedDate,
      startTime: this.selectedSlot,
      patientName: this.patientName,
      patientContact: this.patientContact,
    }).subscribe({
      next: () => {
        this.booking.set(false);
        this.success.set('Programarea a fost înregistrată! O poți vedea în contul tău.');
        this.selectedSlot = null;
        this.refreshSlots();
      },
      error: (err) => {
        this.booking.set(false);
        if (err?.status === 409) {
          this.error.set('Din păcate acest interval tocmai a fost ocupat. Alege alt interval.');
          this.refreshSlots();
        } else {
          this.error.set('Programarea nu a putut fi salvată. Încearcă din nou.');
        }
      },
    });
  }

  goToAccount(): void {
    this.router.navigate(['/account']);
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
