import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

@Schema({ timestamps: true })
export class DailyAttendanceLog extends Document {
  @Prop({ required: true, index: true })
  date: string; // Format: "YYYY-MM-DD"

  @Prop({ required: true, index: true })
  driverId: string;

  @Prop({ type: Types.ObjectId, ref: 'Passenger', required: true, index: true })
  passengerId: Types.ObjectId;

  @Prop({ required: true, enum: ['PENDING', 'PICKED_UP', 'ABSENT'], default: 'PENDING' })
  morningStatus: string;

  @Prop({ required: true, enum: ['PENDING', 'PICKED_UP', 'ABSENT'], default: 'PENDING' })
  eveningStatus: string;
}

export const DailyAttendanceLogSchema = SchemaFactory.createForClass(DailyAttendanceLog);
