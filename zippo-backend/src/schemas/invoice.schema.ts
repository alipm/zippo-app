import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

@Schema({ timestamps: true })
export class Invoice extends Document {
  @Prop({ required: true })
  passengerId: string;

  @Prop({ required: true })
  driverId: string;

  @Prop({ required: true })
  passengerName: string;

  @Prop({ required: true })
  billingMonth: string;

  @Prop({ required: true })
  amount: number;

  @Prop({ default: false })
  isSettled: boolean;

  @Prop()
  paymentMethod?: string;

  @Prop()
  paymentDate?: Date;

  @Prop()
  parentPhoneNumber: string;

  @Prop()
  profileImageBase64: string;
}

export const InvoiceSchema = SchemaFactory.createForClass(Invoice);
