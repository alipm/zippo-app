import {
  Controller,
  Post,
  Get,
  Put,
  Delete,
  Body,
  Headers,
  HttpCode,
  HttpStatus,
  Param,
} from '@nestjs/common';
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
    return this.passengersService.createNewPassengerProfile(
      activeDriverId,
      createPassengerDto,
    );
  }

  @Get('list')
  async getAllPassengers(@Headers('x-driver-id') driverId: string) {
    const passengers = await this.passengersService.findAllPassengers(driverId);
    return passengers;
  }

  @Delete(':id')
  async deletePassenger(
    @Param('id') passengerId: string,
    @Headers('x-driver-id') driverId: string,
  ) {
    return this.passengersService.deletePassenger(driverId, passengerId);
  }

  @Put(':id')
  async updatePassenger(
    @Param('id') passengerId: string,
    @Headers('x-driver-id') driverId: string,
    @Body() dto: CreatePassengerDto,
  ) {

    return this.passengersService.updatePassenger(driverId, passengerId, dto);
  }
}
