import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import { AppointmentDTO, BookAppointmentRequest } from '../entities/appointment';

@Injectable({ providedIn: 'root' })
export class AppointmentApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.baseurl;

  /** Free start times ("HH:mm:ss") for a date/service. Never contains patient identities. */
  availableSlots(date: string, serviceId: string): Observable<string[]> {
    const params = new HttpParams().set('date', date).set('serviceId', serviceId);
    return this.http.get<string[]>(`${this.base}appointments/available-slots`, { params });
  }

  book(request: BookAppointmentRequest): Observable<AppointmentDTO> {
    return this.http.post<AppointmentDTO>(`${this.base}appointments`, request);
  }

  ownAppointments(): Observable<AppointmentDTO[]> {
    return this.http.get<AppointmentDTO[]>(`${this.base}account/appointments`);
  }

  cancelOwn(id: string): Observable<void> {
    return this.http.put<void>(`${this.base}account/appointments/${id}/cancel`, {});
  }

  listAll(): Observable<AppointmentDTO[]> {
    return this.http.get<AppointmentDTO[]>(`${this.base}admin/appointments`);
  }

  updateStatus(id: string, status: string): Observable<AppointmentDTO> {
    return this.http.put<AppointmentDTO>(`${this.base}admin/appointments/${id}/status`, { status });
  }
}
