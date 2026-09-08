import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

@Schema({ timestamps: true })
export class TripSegment extends Document {
  @Prop({ type: Types.ObjectId, required: true, index: true })
  ownerDriverId: Types.ObjectId; // Master driver account who owns the subscriber route

  @Prop({ type: Types.ObjectId, required: true, index: true })
  assignedDriverId: Types.ObjectId; // Changes to a peer driver's ID if delegated

  @Prop({ required: true, index: true })
  date: string; // Format: "YYYY-MM-DD"

  @Prop({ required: true, enum: ['MORNING', 'EVENING'], index: true })
  shift: string;

  @Prop({ required: true })
  tripNumber: number; // e.g., Trip 1, Trip 2, Trip 3

  @Prop({ type: [Types.ObjectId], ref: 'Passenger', required: true })
  passengerIds: Types.ObjectId[]; // The array of passengers inside this specific batch chunk

  @Prop({ required: true, enum: ['ASSIGNED', 'ACCEPTED', 'ACTIVE', 'COMPLETED'], default: 'ASSIGNED' })
  status: string;
}

export const TripSegmentSchema = SchemaFactory.createForClass(TripSegment);
