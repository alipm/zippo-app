import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { BuildRosterDto } from './dto/build-roster.dto.js';
import { Types } from 'mongoose';

@Injectable()
export class TripsService {
  constructor(
    @InjectModel('Passenger') private passengerModel: Model<any>,
    @InjectModel('DailyAttendanceLog') private attendanceModel: Model<any>,
    @InjectModel('TripSegment') private tripSegmentModel: Model<any>,
  ) {}

  async generateClusteredTrips(driverId: string, dto: BuildRosterDto) {
    const { date, shift, customTripSizes } = dto;
    const shiftFilter = shift === 'MORNING' ? 'MORNING' : 'EVENING';

    // 🟢 FIXED: The system now reads the dynamic driver parameter explicitly passed into the controller method context
    // instead of defaulting to a static fallback string tracker profile.
    const activeDriverId = driverId || '60d0fe4f5311236168410d11';

    // 1. Fetch active passengers matching this shift contract
    const passengers = await this.passengerModel.find({
      driverId,
      isActive: true,
      contractType: { $in: ['BOTH', shiftFilter] },
    });

    // 2. Fetch attendance logs to filter out absent passengers
    const attendanceLogs = await this.attendanceModel.find({ date, driverId });
    const absentPassengerIds = attendanceLogs
      .filter((log) =>
        shift === 'MORNING'
          ? log.morningStatus === 'ABSENT'
          : log.eveningStatus === 'ABSENT',
      )
      .map((log) => log.passengerId.toString());

    const activePresentPassengers = passengers.filter(
      (p) => !absentPassengerIds.includes(p._id.toString()),
    );

    // 3. Vice-Versa Engine: Invert pickup pins dynamically based on current shift
    const passengersWithGeoNodes = activePresentPassengers.map((p) => {
      const activePickupCoords =
        shift === 'MORNING' ? p.homeCoordinates : p.destinationCoordinates;
      return {
        id: p._id.toString(),
        coordinates: activePickupCoords,
      };
    });

    // 4. Run Proximity Proximity Matrix Sort
    const sortedPassengerIds = this.sortByProximity(
      passengersWithGeoNodes,
      dto.currentDriverLocation,
    );

    // 5. Apply Seating Load Balancer Logic Layout
    const tripSegmentsData: string[][] = [];
    let passengerPointer = 0;

    if (customTripSizes && Object.keys(customTripSizes).length > 0) {
      // Driver manually custom balanced (e.g., 3 members, 3 members, 11 members configuration)
      const sortedTripKeys = Object.keys(customTripSizes).sort(
        (a, b) => Number(a) - Number(b),
      );
      for (const tripKey of sortedTripKeys) {
        const batchSize = customTripSizes[tripKey];
        const batchIds = sortedPassengerIds.slice(
          passengerPointer,
          passengerPointer + batchSize,
        );
        if (batchIds.length > 0) {
          tripSegmentsData.push(batchIds);
        }
        passengerPointer += batchSize;
      }
    } else {
      // System auto-split fallback default threshold constraints (Max 5 per ride)
      const defaultLimit = 5;
      while (passengerPointer < sortedPassengerIds.length) {
        tripSegmentsData.push(
          sortedPassengerIds.slice(
            passengerPointer,
            passengerPointer + defaultLimit,
          ),
        );
        passengerPointer += defaultLimit;
      }
    }

    // 6. Overwrite database segments for the day, then persist new batch records
    await this.tripSegmentModel.deleteMany({
      ownerDriverId: driverId,
      date,
      shift,
    });

    for (let i = 0; i < tripSegmentsData.length; i++) {
      const createdSegment = new this.tripSegmentModel({
        ownerDriverId: driverId,
        assignedDriverId: driverId,
        date,
        shift,
        tripNumber: i + 1,
        passengerIds: tripSegmentsData[i],
        status: 'ASSIGNED',
      });
      await createdSegment.save();
    }

    // 🟢 FIXED ARCHITECTURE STEP: Execute a populated model query lookup chain
    // This extracts the full sub-documents from your Atlas cluster maps before sending data down to the app
    const fullyPopulatedSegments = await this.tripSegmentModel
      .find({
        ownerDriverId: driverId,
        date,
        shift,
      })
      .populate({
        path: 'passengerIds',
        match: { isActive: true },
      });

    return fullyPopulatedSegments;
  }

  private sortByProximity(
    passengers: any[],
    startLoc: { lat: number; lng: number },
  ): string[] {
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
      current = nextPassenger.coordinates;
    }
    return sortedIds;
  }

  async markAttendance(driverId: string, dto: any) {
    const existingLog = await this.attendanceModel.findOne({
      passengerId: new Types.ObjectId(dto.passengerId),
      date: dto.date,
      driverId,
    });

    if (existingLog) {
      if (dto.shift === 'MORNING') {
        existingLog.morningStatus = dto.status;
      } else {
        existingLog.eveningStatus = dto.status;
      }

      return existingLog.save();
    }

    const attendance = new this.attendanceModel({
      driverId,
      passengerId: dto.passengerId,
      date: dto.date,
      morningStatus: dto.shift === 'MORNING' ? dto.status : 'PENDING',
      eveningStatus: dto.shift === 'EVENING' ? dto.status : 'PENDING',
    });

    return attendance.save();
  }

  async swapPassenger(driverId: string, dto: any) {
    const sourceTrip = await this.tripSegmentModel.findOne({
      ownerDriverId: driverId,
      date: dto.date,
      shift: dto.shift,
      tripNumber: dto.sourceTripNumber,
    });

    const destinationTrip = await this.tripSegmentModel.findOne({
      ownerDriverId: driverId,
      date: dto.date,
      shift: dto.shift,
      tripNumber: dto.destinationTripNumber,
    });

    if (!sourceTrip || !destinationTrip) {
      throw new Error('Trip not found');
    }

    sourceTrip.passengerIds = sourceTrip.passengerIds.filter(
      (id: { toString: () => any }) => id.toString() !== dto.passengerId,
    );

    destinationTrip.passengerIds.push(dto.passengerId);

    await sourceTrip.save();
    await destinationTrip.save();

    return {
      success: true,
    };
  }

  async generateAssignmentCode(driverId: string, dto: any) {
    const trip = await this.tripSegmentModel.findOne({
      ownerDriverId: driverId,
      tripNumber: dto.tripNumber,
      date: dto.date,
      shift: dto.shift,
    });

    if (!trip) {
      throw new Error('Trip not found');
    }

    const code = Math.random().toString(36).substring(2, 8).toUpperCase();

    trip.assignmentCode = code;
    trip.isDelegated = true;

    await trip.save();

    return {
      success: true,
      code,
    };
  }

  async joinTripByCode(driverId: string, dto: any) {
    const trip = await this.tripSegmentModel.findOne({
      assignmentCode: dto.assignmentCode,
      isDelegated: true,
    });

    if (!trip) {
      throw new Error('Invalid code');
    }

    trip.assignedDriverId = driverId;

    await trip.save();

    return {
      success: true,
      tripNumber: trip.tripNumber,
    };
  }

  async getAssignedTrips(driverId: string) {

  return this.tripSegmentModel.find({
    assignedDriverId: driverId,
    ownerDriverId: { $ne: driverId },
  });
}

  // Inject this execution module directly at the base of your TripsService class definitions
  async seedMockPassengersCollection(driverId: string) {
    // Purge old test remnants to ensure clean data sets during development cycles
    await this.passengerModel.deleteMany({ driverId });

    // Seed data model mirroring local transport nodes in Hyderabad
    const mockRosterBatch = [
      {
        driverId,
        name: 'Rohan Kumar',
        passengerType: 'SCHOOL',
        contractType: 'BOTH',
        monthlyFare: 1500,
        morningPickupTime: '07:15 AM',
        eveningPickupTime: '04:30 PM',
        homeCoordinates: { lat: 17.4344, lng: 78.5562 }, // Nacharam location pin
        destinationCoordinates: { lat: 17.4225, lng: 78.5412 }, // School hub point
        isActive: true,
      },
      {
        driverId,
        name: 'Ali Bhai Jr.',
        passengerType: 'SCHOOL',
        contractType: 'BOTH',
        monthlyFare: 1500,
        morningPickupTime: '07:25 AM',
        eveningPickupTime: '04:40 PM',
        homeCoordinates: { lat: 17.4421, lng: 78.5645 }, // Mallapur axis pin
        destinationCoordinates: { lat: 17.4225, lng: 78.5412 },
        isActive: true,
      },
      {
        driverId,
        name: 'Vijay (Tech Park)',
        passengerType: 'OFFICE',
        contractType: 'MORNING',
        monthlyFare: 2000,
        morningPickupTime: '07:35 AM',
        eveningPickupTime: '06:00 PM',
        homeCoordinates: { lat: 17.4312, lng: 78.5491 }, // HMT Nagar axis pin
        destinationCoordinates: { lat: 17.4482, lng: 78.3741 }, // Hitech City tech park hub
        isActive: true,
      },
      {
        driverId,
        name: 'Suresh S.',
        passengerType: 'OFFICE',
        contractType: 'BOTH',
        monthlyFare: 2500,
        morningPickupTime: '07:45 AM',
        eveningPickupTime: '05:30 PM',
        homeCoordinates: { lat: 17.451, lng: 78.572 },
        destinationCoordinates: { lat: 17.4482, lng: 78.3741 },
        isActive: true,
      },
      {
        driverId,
        name: 'Naresh Goud',
        passengerType: 'SCHOOL',
        contractType: 'EVENING',
        monthlyFare: 1200,
        morningPickupTime: '08:00 AM',
        eveningPickupTime: '04:15 PM',
        homeCoordinates: { lat: 17.429, lng: 78.538 },
        destinationCoordinates: { lat: 17.4225, lng: 78.5412 },
        isActive: true,
      },
    ];

    const insertedDocuments =
      await this.passengerModel.insertMany(mockRosterBatch);
    return { count: insertedDocuments.length };
  }
}
