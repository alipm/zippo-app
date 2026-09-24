import {
  Controller,
  Get,
  Patch,
  Body,
  Headers,
  HttpCode,
  HttpStatus,
  Query,
} from '@nestjs/common';
import { PaymentsService } from './payments.service.js';

@Controller('payments')
export class PaymentsController {
  constructor(private readonly paymentsService: PaymentsService) {}

  @Get('ledger-summary')
  async getLedgerSummary(
    @Query('driverId') driverId: string,
    @Query('month') month?: string,
  ) {
    return this.paymentsService.getMonthlyLedgerSummary(driverId, month);
  }

  // 2. CLEAR BALANCE INVOICE OVERRIDES (PATCH: http://localhost:3000/payments/update-status)
  @Patch('update-status')
  @HttpCode(HttpStatus.OK)
  async updateStatus(
    @Headers('x-driver-id') driverId: string,
    @Body() body: { invoiceId: string; paymentType: string },
  ) {
    const activeDriverId = driverId || '60d0fe4f5311236168410d11';
    return this.paymentsService.processPaymentSettlement(
      activeDriverId,
      body.invoiceId,
      body.paymentType,
    );
  }

  @Get('available-months')
  async getAvailableMonths(@Query('driverId') driverId: string) {
    return this.paymentsService.getAvailableMonths(driverId);
  }
}
