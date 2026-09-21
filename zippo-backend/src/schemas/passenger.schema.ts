import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

@Schema({ timestamps: true })
export class Passenger extends Document {
  @Prop({ type: Types.ObjectId, required: true })
  driverId: Types.ObjectId;

  @Prop({ required: true, index: true })
  name: string;

  @Prop({ type: Types.ObjectId, default: null, index: true })
  parentGroupId: Types.ObjectId; // Links multiple siblings to one parent profile for unified billing

  @Prop({ required: true, enum: ['SCHOOL', 'OFFICE'] })
  passengerType: string;

  @Prop({ required: true, enum: ['BOTH', 'MORNING', 'EVENING'], default: 'BOTH' })
  contractType: string; // Toggles whether they appear in morning, evening, or both rosters

  @Prop({ required: true })
  monthlyFare: number;

  @Prop({ required: true })
  morningPickupTime: string; // Format: "07:15 AM"

  @Prop({ required: true })
  eveningPickupTime: string; // Format: "04:30 PM"

  @Prop({ type: Object, required: true })
  homeCoordinates: { lat: number; lng: number }; // Set once at client gate

  @Prop({ type: Object, required: true })
  destinationCoordinates: { lat: number; lng: number }; // Set once at school/office hub

  @Prop({ default: true })
  isActive: boolean; // Soft delete tracking flag

  @Prop({ required: false, default: null })
  profileImageBase64: string;

  @Prop({ required: true })
  phoneNumber: string;
}

export const PassengerSchema = SchemaFactory.createForClass(Passenger);
