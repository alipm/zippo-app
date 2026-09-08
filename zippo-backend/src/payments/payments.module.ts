import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { PaymentsController } from './payments.controller.js';
import { PaymentsService } from './payments.service.js';
import { PassengerSchema } from '../schemas/passenger.schema.js';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: 'Passenger', schema: PassengerSchema }]),
  ],
  controllers: [PaymentsController],
  providers: [PaymentsService],
})
export class PaymentsModule {}
