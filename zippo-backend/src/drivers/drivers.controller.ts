import {
  Body,
  Controller,
  HttpCode,
  HttpStatus,
  Post,
  Get,
} from '@nestjs/common';

import { DriversService } from './drivers.service.js';

@Controller('drivers')
export class DriversController {
  constructor(private readonly driversService: DriversService) {}

  @Post('register')
  @HttpCode(HttpStatus.CREATED)
  async registerDriver(@Body() dto: any) {
    const result = await this.driversService.registerDriver(dto);

    return {
      success: true,
      data: result,
    };
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  async loginDriver(@Body() dto: any) {
    const result = await this.driversService.loginDriver(dto);

    return {
      success: true,
      data: result,
    };
  }

  @Get('test-email')
  async testEmail() {
    return this.driversService.sendTestEmail();
  }

  @Post('forgot-password')
  async forgotPassword(@Body() dto: any) {
    return this.driversService.forgotPassword(dto);
  }

  @Post('reset-password')
  async resetPassword(@Body() dto: any) {
    return this.driversService.resetPassword(dto);
  }
}
