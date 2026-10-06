export class MarkAttendanceDto {
  passengerId: string;
  date: string;
  shift: 'MORNING' | 'EVENING';
  status: 'PICKED_UP' | 'ABSENT';
}