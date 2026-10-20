import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';

import { DriversController } from './drivers.controller.js';
import { DriversService } from './drivers.service.js';

import {
  DriverSchema,
} from '../schemas/driver.schema.js';

@Module({
  imports: [
    MongooseModule.forFeature([
      {
        name: 'Driver',
        schema: DriverSchema,
      },
    ]),
  ],
  controllers: [
    DriversController,
  ],
  providers: [
    DriversService,
  ],
})
export class DriversModule {}
