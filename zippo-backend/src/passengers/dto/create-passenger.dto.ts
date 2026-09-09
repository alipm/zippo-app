import { IsNotEmpty, IsString, IsEnum, IsNumber, IsObject } from 'class-validator';

export class CreatePassengerDto {
  @IsNotEmpty()
  @IsString()
  name: string;

  @IsNotEmpty()
  @IsEnum(['SCHOOL', 'OFFICE'])
  passengerType: string;

  @IsNotEmpty()
  @IsEnum(['BOTH', 'MORNING_ONLY', 'EVENING_ONLY'])
  contractType: string;

  @IsNotEmpty()
  @IsNumber()
  monthlyFare: number;

  @IsNotEmpty()
  @IsString()
  morningPickupTime: string;

  @IsNotEmpty()
  @IsString()
  eveningPickupTime: string;

  @IsNotEmpty()
  @IsObject()
  homeCoordinates: { lat: number; lng: number };

  @IsNotEmpty()
  @IsObject()
  destinationCoordinates: { lat: number; lng: number };
}
