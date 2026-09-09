import { Controller, Get, Patch, Body, Headers, HttpCode, HttpStatus } from '@nestjs/common';
import { PaymentsService } from './payments.service.js';

@Controller('payments')
export class PaymentsController {
  constructor(private readonly paymentsService: PaymentsService) {}

  // 🟢 1. FETCH LIVE CALCULATED OVERVIEW (GET: http://localhost:3000/payments/ledger-summary)
  @Get('ledger-summary')
  @HttpCode(HttpStatus.OK)
  async getSummary(@Headers('x-driver-id') driverId: string) {
    const activeDriverId = driverId || '60d0fe4f5311236168410d11';
    return this.paymentsService.getMonthlyLedgerSummary(activeDriverId);
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
}
