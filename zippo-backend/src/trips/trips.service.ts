// src/trips/trips.service.ts
import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Passenger } from '../schemas/passenger.schema.js';
import { DailyAttendanceLog } from '../schemas/attendance.schema.js';
import { TripSegment } from '../schemas/trip-segment.schema.js';
import { BuildRosterDto } from './dto/build-roster.dto.js';

@Injectable()
export class TripsService {
  constructor(
    @InjectModel('Passenger') private passengerModel: Model<Passenger>,
    @InjectModel('DailyAttendanceLog') private attendanceModel: Model<DailyAttendanceLog>,
    @InjectModel('TripSegment') private tripSegmentModel: Model<TripSegment>,
  ) {}

  async generateClusteredTrips(driverId: string, dto: BuildRosterDto) {
    const { date, shift, customTripSizes } = dto;

    // 1. QUERY ALL ACTIVE PASSENGERS BELONGING TO THIS DRIVER AND THE ACTIVE SHIFT CONTRACT
    const shiftFilter = shift === 'MORNING' ? 'MORNING_ONLY' : 'EVENING_ONLY';
    const passengers = await this.passengerModel.find({
      driverId,
      isActive: true,
      contractType: { $in: ['BOTH', shiftFilter] },
    });

    // 2. QUERY TODAY'S ATTENDANCE RECORDS TO FILTER OUT ABSENT PASSENGERS BEFORE GENERATION
    const attendanceLogs = await this.attendanceModel.find({ date });
    const absentPassengerIds = attendanceLogs
      .filter((log) => (shift === 'MORNING' ? log.morningStatus === 'ABSENT' : log.eveningStatus === 'ABSENT'))
      .map((log) => log.passengerId.toString());

    // 3. SECURE EXTRACT OF PASSENGERS WHO ARE ACTIVE AND PRESENT FOR THIS RUN
    const activePresentPassengers = passengers.filter(
      (p) => !absentPassengerIds.includes(p._id.toString()),
    );

    // 4. VICE-VERSA COORDINATE INVERSION FILTER INTERACTION
    // Map geometry nodes dynamically: Morning targets home gate pickup, Evening targets destination hub pickup
    const passengersWithGeoNodes = activePresentPassengers.map((p) => {
      const activePickupCoords = shift === 'MORNING' ? p.homeCoordinates : p.destinationCoordinates;
      return {
        id: p._id.toString(),
        name: p.name,
        coordinates: activePickupCoords,
      };
    });

    // 5. CALL OPTIMIZED SORT ENGINE / AI AGENT HOOK
    // For local fallback testing, we sort by absolute distance proximity relative to the driver's start vector
    const sortedPassengerIds = this.sortByProximity(passengersWithGeoNodes, dto.currentDriverLocation);

    // 6. CAPACITY BATCH SHEET SPLITTING MATRIX (Automated vs Uneven Custom Sizes Override)
    const tripSegmentsData: string[][] = [];
    let passengerPointer = 0;

    if (customTripSizes && Object.keys(customTripSizes).length > 0) {
      // Driver manually tailored capacity configuration layout (e.g., Batch 1: 3, Batch 2: 3, Batch 3: 11)
      const sortedTripKeys = Object.keys(customTripSizes).sort((a, b) => Number(a) - Number(b));
      
      for (const tripKey of sortedTripKeys) {
        const batchSize = customTripSizes[tripKey];
        const batchIds = sortedPassengerIds.slice(passengerPointer, passengerPointer + batchSize);
        if (batchIds.length > 0) {
          tripSegmentsData.push(batchIds);
        }
        passengerPointer += batchSize;
      }
    } else {
      // Global fallback sorting rule chunk defaults if no manual balances exist (Max 5 per row)
      const defaultLimit = 5;
      while (passengerPointer < sortedPassengerIds.length) {
        tripSegmentsData.push(sortedPassengerIds.slice(passengerPointer, passengerPointer + defaultLimit));
        passengerPointer += defaultLimit;
      }
    }

    // 7. PURGE OLD SEGMENTS FOR THIS DAY & SHIFT, THEN PERSIST NEW BATCH CONFIGS TO MONGO
    await this.tripSegmentModel.deleteMany({ driverId, date, shift });

    const persistedSegments = [];
    for (let i = 0; i < tripSegmentsData.length; i++) {
      const createdSegment = new this.tripSegmentModel({
        ownerDriverId: driverId,
        assignedDriverId: driverId, // Defaults to owner account, flips if delegated later
        date,
        shift,
        tripNumber: i + 1,
        passengerIds: tripSegmentsData[i],
        status: 'ASSIGNED',
      });
      persistedSegments.push(await createdSegment.save());
    }

    return persistedSegments;
  }

  // Pure geometric calculations algorithm tracking distance vectors safely
  private sortByProximity(passengers: any[], startLoc: { lat: number; lng: number }): string[] {
    let current = startLoc;
    const remaining = [...passengers];
    const sortedIds: string[] = [];

    while (remaining.length > 0) {
      let closestIdx = 0;
      let minDistance = Infinity;

      for (let i = 0; i < remaining.length; i++) {
        const distance = Math.sqrt(
          Math.pow(remaining[i].coordinates.lat - current.lat, 2) +
          Math.pow(remaining[i].coordinates.lng - current.lng, 2),
        );
        if (distance < minDistance) {
          minDistance = distance;
          closestIdx = i;
        }
      }

      const nextPassenger = remaining.splice(closestIdx, 1)[0];
      sortedIds.push(nextPassenger.id);
      current = nextPassenger.coordinates; // Move tracking vector forward to next stop
    }

    return sortedIds;
  }
}
