import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import * as bcrypt from 'bcrypt';
import * as nodemailer from 'nodemailer';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class DriversService {
  constructor(
    @InjectModel('Driver')
    private driverModel: Model<any>,
  ) {}

  async registerDriver(dto: any) {
    const existingDriver = await this.driverModel.findOne({
      email: dto.email,
    });

    if (existingDriver) {
      throw new Error('Driver already exists');
    }

    const passwordHash = await bcrypt.hash(dto.password, 10);

    const driver = new this.driverModel({
      name: dto.name,
      phoneNumber: dto.phoneNumber,
      email: dto.email,
      vehicleNumber: dto.vehicleNumber,
      role: dto.role,
      passwordHash,
    });

    return driver.save();
  }

  async loginDriver(dto: any) {
    const driver = await this.driverModel.findOne({
      email: dto.email,
      isActive: true,
    });

    if (!driver) {
      throw new Error('Invalid credentials');
    }

    const valid = await bcrypt.compare(dto.password, driver.passwordHash);

    if (!valid) {
      throw new Error('Invalid credentials');
    }

    return driver;
  }

  private transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: process.env.SMTP_EMAIL,
      pass: process.env.SMTP_PASSWORD,
    },
  });

  async sendTestEmail() {
    await this.transporter.sendMail({
      to: process.env.SMTP_EMAIL,
      subject: 'ZIPPO Test',
      text: 'Test email from ZIPPO',
    });

    return {
      success: true,
    };
  }

  async forgotPassword(dto: any) {
    const driver = await this.driverModel.findOne({
      email: dto.email,
    });

    if (!driver) {
      return {
        success: true,
      };
    }

    const token = uuidv4();
    driver.resetToken = token;
    driver.resetTokenExpiry = new Date(Date.now() + 30 * 60 * 1000);
    await driver.save();

    await this.transporter.sendMail({
      to: driver.email,
      subject: 'ZIPPO Password Reset',
      text: `Your ZIPPO password reset token: ${token} Valid for 30 minutes.`,
    });

    return {
      success: true,
    };
  }

  async resetPassword(dto: any) {
    const driver = await this.driverModel.findOne({
      resetToken: dto.token,
    });

    if (!driver) {
      throw new Error('Invalid token');
    }

    if (!driver.resetTokenExpiry || driver.resetTokenExpiry < new Date()) {
      throw new Error('Token expired');
    }

    driver.passwordHash = await bcrypt.hash(dto.newPassword, 10);

    driver.resetToken = null;
    driver.resetTokenExpiry = null;

    await driver.save();

    return {
      success: true,
    };
  }
}
