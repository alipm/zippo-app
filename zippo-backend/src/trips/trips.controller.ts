import { Controller, Post, Body, Headers, HttpCode, HttpStatus } from '@nestjs/common';
import { TripsService } from './trips.service.js';
import { BuildRosterDto } from './dto/build-roster.dto.js';

@Controller('trips')
export class TripsController {
  constructor(private readonly tripsService: TripsService) {}

  // POST endpoint: http://localhost:3000/trips/build-roster
  @Post('build-roster')
  @HttpCode(HttpStatus.OK)
  async buildDailyRoster(
    @Body() buildRosterDto: BuildRosterDto,
    @Headers('x-driver-id') driverId: string, // Temporary test header variable to represent active user context
  ) {
    // Falls back to a valid MongoDB object ID format for local testing if header is empty
    const activeDriverId = driverId || '60d0fe4f5311236168410d11';
    
    const segments = await this.tripsService.generateClusteredTrips(activeDriverId, buildRosterDto);
    
    return {
      success: true,
      message: 'Shift daily route sequence calculated and saved successfully.',
      data: segments,
    };
  }
}
