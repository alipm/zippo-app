import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

@Schema({ timestamps: true })
export class Driver extends Document {
  @Prop({ required: true })
  name: string;

  @Prop({ required: true, unique: true })
  phoneNumber: string;

  @Prop({ required: true, default: true })
  isActive: boolean;

  @Prop({ type: Object, default: { lat: 17.4344, lng: 78.5562 } }) // Defaults to your main Nacharam staging coordinate hub
  lastKnownLocation: { lat: number; lng: number };
}

export const DriverSchema = SchemaFactory.createForClass(Driver);
