import { Module } from '@nestjs/common';
import { AiController } from './ai.controller.js';
import { AiService } from './ai.service.js';

@Module({
  controllers: [AiController],
  providers: [AiService],
  exports: [AiService] // Exports it so the Trips Module can access our routing optimizer
})
export class AiModule {}
