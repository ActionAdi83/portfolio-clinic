import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import { MedicalServiceDTO } from '../entities/medical-service';

@Injectable({ providedIn: 'root' })
export class MedicalServiceApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.baseurl;

  /** Public catalogue — active services only. */
  listActive(): Observable<MedicalServiceDTO[]> {
    return this.http.get<MedicalServiceDTO[]>(`${this.base}services`);
  }

  /** Admin: every service, active or not. */
  listAll(): Observable<MedicalServiceDTO[]> {
    return this.http.get<MedicalServiceDTO[]>(`${this.base}admin/services`);
  }

  create(dto: MedicalServiceDTO): Observable<MedicalServiceDTO> {
    return this.http.post<MedicalServiceDTO>(`${this.base}admin/services`, dto);
  }

  update(id: string, dto: MedicalServiceDTO): Observable<MedicalServiceDTO> {
    return this.http.put<MedicalServiceDTO>(`${this.base}admin/services/${id}`, dto);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}admin/services/${id}`);
  }

  /** Uploads a photo from disk; the returned `url` is what goes into imageUrl. */
  uploadImage(file: File): Observable<{ id: string; url: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ id: string; url: string }>(`${this.base}admin/services/images`, form);
  }
}
