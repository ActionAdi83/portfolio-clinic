export type DayOfWeek =
  | 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const DAYS_OF_WEEK: DayOfWeek[] = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
];

export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Luni',
  TUESDAY: 'Marți',
  WEDNESDAY: 'Miercuri',
  THURSDAY: 'Joi',
  FRIDAY: 'Vineri',
  SATURDAY: 'Sâmbătă',
  SUNDAY: 'Duminică',
};

export interface ScheduleDayDTO {
  dayOfWeek: DayOfWeek;
  active: boolean;
  startTime: string; // "HH:mm:ss" as returned by Spring for a LocalTime
  endTime: string;
  slotDurationMinutes: number;
}
