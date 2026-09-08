// src/trips/dto/build-roster.dto.ts
import { IsNotEmpty, IsEnum, IsString, IsObject, IsOptional } from 'class-validator';

export class BuildRosterDto {
  @IsNotEmpty()
  @IsString()
  date: string; // Format: "YYYY-MM-DD"

  @IsNotEmpty()
  @IsEnum(['MORNING', 'EVENING'])
  shift: string;

  @IsNotEmpty()
  @IsObject()
  currentDriverLocation: { lat: number; lng: number };

  @IsOptional()
  @IsObject()
  customTripSizes?: { [tripNumber: string]: number }; // e.g., { "1": 3, "2": 3, "3": 11 }
}
