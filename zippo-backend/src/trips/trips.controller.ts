import {
  Controller,
  Post,
  Body,
  Headers,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { TripsService } from './trips.service.js';
import { BuildRosterDto } from './dto/build-roster.dto.js';
import { MarkAttendanceDto } from './dto/mark-attendance.dto.js';

@Controller('trips')
export class TripsController {
  constructor(private readonly tripsService: TripsService) {}

  // 1. EXISTENT ROSTER BUILDER ENDPOINT
  @Post('build-roster')
  @HttpCode(HttpStatus.OK)
  async buildDailyRoster(
    @Body() buildRosterDto: BuildRosterDto,
    @Headers('x-driver-id') driverId: string,
  ) {
    const activeDriverId = driverId || '60d0fe4f5311236168410d11';
    const segments = await this.tripsService.generateClusteredTrips(
      activeDriverId,
      buildRosterDto,
    );
    return {
      success: true,
      message: 'Shift daily route sequence calculated and saved successfully.',
      data: segments,
    };
  }

  @Post('mark-attendance')
  @HttpCode(HttpStatus.OK)
  async markAttendance(
    @Body() dto: MarkAttendanceDto,
    @Headers('x-driver-id') driverId: string,
  ) {
    const activeDriverId = driverId || '60d0fe4f5311236168410d11';
    const result = await this.tripsService.markAttendance(activeDriverId, dto);
    return {
      success: true,
      message: 'Attendance updated successfully.',
      data: result,
    };
  }

  // 2. NEW PRODUCTION SEEDER ENDPOINT (POST: http://localhost:3000/trips/seed-passengers)
  @Post('seed-passengers')
  @HttpCode(HttpStatus.CREATED)
  async seedMockDatabaseRecords() {
    const testDriverId = '60d0fe4f5311236168410d11'; // Synchronized testing ID token
    const result =
      await this.tripsService.seedMockPassengersCollection(testDriverId);
    return {
      success: true,
      message: 'MongoDB Atlas cloud documents successfully hydrated.',
      count: result.count,
    };
  }
}