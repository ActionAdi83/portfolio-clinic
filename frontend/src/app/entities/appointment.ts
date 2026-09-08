export type AppointmentStatus = 'REQUESTED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';

export interface AppointmentDTO {
  id: string;
  serviceId: string;
  serviceTitle: string;
  date: string; // "YYYY-MM-DD"
  startTime: string; // "HH:mm:ss"
  endTime: string;
  status: AppointmentStatus;
  createdAt: string;
  patientName?: string;
  patientContact?: string;
}

export interface BookAppointmentRequest {
  serviceId: string;
  date: string;
  startTime: string;
  patientName: string;
  patientContact: string;
}
