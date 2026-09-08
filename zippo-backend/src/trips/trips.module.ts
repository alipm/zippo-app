import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { TripsController } from './trips.controller.js';
import { TripsService } from './trips.service.js';
import { PassengerSchema } from '../schemas/passenger.schema.js';
import { DailyAttendanceLogSchema } from '../schemas/attendance.schema.js';
import { TripSegmentSchema } from '../schemas/trip-segment.schema.js';

@Module({
  imports: [
    // Binds your database collection models to this standalone framework context
    MongooseModule.forFeature([
      { name: 'Passenger', schema: PassengerSchema },
      { name: 'DailyAttendanceLog', schema: DailyAttendanceLogSchema },
      { name: 'TripSegment', schema: TripSegmentSchema },
    ]),
  ],
  controllers: [TripsController],
  providers: [TripsService],
})
export class TripsModule {}
