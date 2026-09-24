import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';

@Injectable()
export class PaymentsService {
  constructor(
    @InjectModel('Passenger')
    private passengerModel: Model<any>,

    @InjectModel('Invoice')
    private invoiceModel: Model<any>,
  ) {}

  async getMonthlyLedgerSummary(driverId: string, month?: string) {
    await this.generateMonthlyInvoices(driverId);

    const billingMonth = month || new Date().toISOString().slice(0, 7);

    const invoices = await this.invoiceModel.find({
      driverId,
      billingMonth: billingMonth,
    });

    let totalCollected = 0;
    let pendingDues = 0;

    let paidCount = 0;
    let dueCount = 0;

    const invoiceResponses = invoices.map((invoice) => {
      if (invoice.isSettled) {
        totalCollected += invoice.amount;
        paidCount++;
      } else {
        pendingDues += invoice.amount;
        dueCount++;
      }

      return {
        id: invoice._id.toString(),
        householdName: invoice.passengerName,
        description: `Monthly Transport Fee (${invoice.billingMonth})`,
        fareAmount: invoice.amount,
        isSettled: invoice.isSettled,
        parentPhoneNumber: invoice.parentPhoneNumber,
        profileImageBase64: invoice.profileImageBase64,
        paymentDate: invoice.paymentDate,
      };
    });

    return {
      success: true,
      message: 'Monthly Ledger Loaded',
      totalCollected,
      pendingDues,
      paidCount,
      dueCount,
      invoices: invoiceResponses,
    };
  }

  async processPaymentSettlement(
    driverId: string,
    invoiceId: string,
    paymentType: string,
  ) {
    const targetInvoice = await this.invoiceModel.findOne({
      _id: invoiceId,
      driverId,
    });

    if (!targetInvoice) {
      throw new NotFoundException('Invoice not found');
    }

    await this.invoiceModel.updateOne(
      {
        _id: invoiceId,
      },
      {
        $set: {
          isSettled: true,
          paymentMethod: paymentType,
          paymentDate: new Date(),
        },
      },
    );

    return {
      success: true,
      message: `Payment received via ${paymentType}`,
    };
  }

  async generateMonthlyInvoices(driverId: string) {
    const currentMonth = new Date().toISOString().slice(0, 7);

    const passengers = await this.passengerModel.find({
      driverId,
      isActive: true,
    });

    for (const passenger of passengers) {
      const existingInvoice = await this.invoiceModel.findOne({
        passengerId: passenger._id.toString(),
        billingMonth: currentMonth,
      });

      if (existingInvoice) {
        continue;
      }

      await this.invoiceModel.create({
        passengerId: passenger._id.toString(),
        driverId,
        passengerName: passenger.name,
        billingMonth: currentMonth,
        amount: passenger.monthlyFare,
        isSettled: false,
        parentPhoneNumber: passenger.phoneNumber,
        profileImageBase64: passenger.profileImageBase64,
      });
    }
  }

  async getAvailableMonths(driverId: string) {
    const months = await this.invoiceModel.distinct('billingMonth', {
      driverId,
    });

    return months.sort().reverse();
  }
}
