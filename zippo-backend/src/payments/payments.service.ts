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

  async getMonthlyLedgerSummary(driverId: string) {

    await this.generateMonthlyInvoices(driverId);
    // Extract all live subscriber assets matching the driver's profile context
    const passengers = await this.passengerModel.find({
      driverId,
      isActive: true,
    });

    let totalCollected = 0;
    let pendingDues = 0;

    // Run dynamic aggregation pipelines natively across the documents array
    const invoices = passengers.map((passenger) => {
      const isPassengerSettled = passenger.paymentSettled ?? false;
      const fare = passenger.monthlyFare || 1500;

      if (isPassengerSettled) {
        totalCollected += fare;
      } else {
        pendingDues += fare;
      }

      return {
        id: passenger._id.toString(),
        householdName: passenger.name || 'Subscriber Passenger Profile',
        description: `${passenger.passengerType || 'SCHOOL'} Route Contract - ${passenger.contractType || 'BOTH'}`,
        fareAmount: fare,
        isSettled: isPassengerSettled,
        parentPhoneNumber: passenger.phoneNumber || '', // Fallback communication anchor
      };
    });

    return {
      success: true,
      message:
        'Financial collections summary compiled cleanly from cloud collections.',
      totalCollected,
      pendingDues,
      invoices,
    };
  }

  async processPaymentSettlement(
    driverId: string,
    invoiceId: string,
    paymentType: string,
  ) {
    const targetPassenger = await this.passengerModel.findOne({
      _id: invoiceId,
      driverId,
    });

    if (!targetPassenger) {
      throw new NotFoundException('Passenger not found');
    }

    await this.passengerModel.updateOne(
      {
        _id: invoiceId,
        driverId,
      },
      {
        $set: {
          paymentSettled: true,
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

   const invoices = await this.invoiceModel.find({
      driverId,
      billingMonth: currentMonth
   });

    for (const passenger of invoices) {
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
      });
    }
  }
}
