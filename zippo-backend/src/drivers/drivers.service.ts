import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import * as bcrypt from 'bcrypt';
import * as nodemailer from 'nodemailer';

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
    // host: 'smtp-relay.brevo.com',
    // port: 587,
    // secure: false,
    host: 'smtp.gmail.com',
    port: 587,
    secure: false,
    requireTLS: true,
    auth: {
      user: process.env.SMTP_EMAIL,
      pass: process.env.SMTP_PASSWORD,
    },
  });

  async sendTestEmail() {
    console.log('SMTP_EMAIL =', process.env.SMTP_EMAIL);
    console.log('SMTP_PASSWORD EXISTS =', !!process.env.SMTP_PASSWORD);
    try {
      await this.transporter.verify();
      console.log('SMTP VERIFIED');
      await this.transporter.sendMail({
        to: process.env.SMTP_EMAIL,
        subject: 'ZIPPO Test',
        text: 'Test email from ZIPPO',
      });
      console.log('EMAIL SENT');
    } catch (error) {
      console.error('SMTP ERROR', JSON.stringify(error, null, 2));
      throw error;
    }
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

    const token = Math.floor(100000 + Math.random() * 900000).toString();
    driver.resetToken = token;
    driver.resetTokenExpiry = new Date(Date.now() + 15 * 60 * 1000);
    await driver.save();

    await this.transporter.sendMail({
      from: 'ZIPPO <masoom117@gmail.com>',
      to: driver.email,
      subject: 'Your ZIPPO Verification Code',
      html: `
<div style="font-family:Arial">

<h2>Password Reset Request</h2>

<p>Hello ${driver.name},</p>

<p>Your verification code is:</p>

<h1 style="color:#1976D2">${token}</h1>

<p>
This code is valid for 15 minutes.
</p>

<p>
If you did not request this reset,
please ignore this email.
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
