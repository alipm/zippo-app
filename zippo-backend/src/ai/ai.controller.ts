import { Controller, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import { AiService } from './ai.service.js';

@Controller('ai')
export class AiController {
  constructor(private readonly aiService: AiService) {}

  // POST Endpoint: http://localhost:3000/ai/voice-khata
  @Post('voice-khata')
  @HttpCode(HttpStatus.OK)
  async processVoiceEntry(@Body() body: { transcript: string }) {
    const parsedLog = await this.aiService.parseVoiceKhataTranscript(body.transcript);
    return {
      success: true,
      message: 'Voice Bahi-Khata ledger command parsed successfully.',
      data: parsedLog
    };
  }
}
