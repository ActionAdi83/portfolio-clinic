import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import { ScheduleDayDTO } from '../entities/schedule-day';

@Injectable({ providedIn: 'root' })
export class ScheduleApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.baseurl;

  get(): Observable<ScheduleDayDTO[]> {
    return this.http.get<ScheduleDayDTO[]>(`${this.base}admin/schedule`);
  }

  update(days: ScheduleDayDTO[]): Observable<ScheduleDayDTO[]> {
    return this.http.put<ScheduleDayDTO[]>(`${this.base}admin/schedule`, days);
  }
}
