import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { CreatePassengerDto } from './dto/create-passenger.dto.js';

@Injectable()
export class PassengersService {
  constructor(
    @InjectModel('Passenger') private passengerModel: Model<any>,
  ) {}

  async createNewPassengerProfile(driverId: string, dto: CreatePassengerDto) {
    // Instantiate a new document mapping to your Passenger collection model schema
    const newPassenger = new this.passengerModel({
      driverId,
      ...dto,
      parentGroupId: null, // Initialized as independent billing profile, bundles later
      isActive: true,
    });

    const savedRecord = await newPassenger.save();

    return {
      success: true,
      message: 'New subscriber profile permanently persisted to MongoDB Atlas cloud.',
      passengerId: savedRecord._id.toString(),
    };
  }
}
