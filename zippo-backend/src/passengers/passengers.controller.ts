import { Controller, Post, Body, Headers, HttpCode, HttpStatus } from '@nestjs/common';
import { PassengersService } from './passengers.service.js';
import { CreatePassengerDto } from './dto/create-passenger.dto.js';

@Controller('passengers')
export class PassengersController {
  constructor(private readonly passengersService: PassengersService) {}

  // POST endpoint: http://localhost:3000/passengers/create
  @Post('create')
  @HttpCode(HttpStatus.CREATED)
  async registerPassenger(
    @Headers('x-driver-id') driverId: string,
    @Body() createPassengerDto: CreatePassengerDto,
  ) {
    const activeDriverId = driverId || '60d0fe4f5311236168410d11';
    return this.passengersService.createNewPassengerProfile(activeDriverId, createPassengerDto);
  }
}
