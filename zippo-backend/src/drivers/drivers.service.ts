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
    const email = dto.email.trim().toLowerCase();
    const existingDriver = await this.driverModel.findOne({
      email: email,
    });

    if (existingDriver) {
      throw new Error('Driver already exists');
    }

    const passwordHash = await bcrypt.hash(dto.password, 10);

    const driver = new this.driverModel({
      name: dto.name,
      phoneNumber: dto.phoneNumber,
      email: email,
      vehicleNumber: dto.vehicleNumber,
      passwordHash,
    });

    return driver.save();
  }

  async loginDriver(dto: any) {
    const email = dto.email.trim().toLowerCase();
    const driver = await this.driverModel.findOne({
      email: email,
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

      html: `
   <div style="font-family: Arial, sans-serif;">
      <h2>ZIPPO Password Reset</h2>

      <p>Hello ${driver.name},</p>

      <p>
      A request was received to reset your ZIPPO account password.
      </p>

      <p>
      Use the following reset token:
      </p>

      <h3 style="color:#1976D2;">
      ${token}
      </h3>

      <p>
      This token is valid for 30 minutes.
      </p>

      <p>
      If you did not request this password reset,
      you can safely ignore this email.
      </p>

      <br/>

      <p>
      ZIPPO Transport Management System
      </p>
  </div>
  `,
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
