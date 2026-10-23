import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import * as bcrypt from 'bcrypt';
import { Resend } from 'resend';

@Injectable()
export class DriversService {
  private resend = new Resend(process.env.RESEND_API_KEY);

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

  async sendTestEmail() {
    const result = await this.resend.emails.send({
      from: 'onboarding@resend.dev',

      to: 'masoom117@gmail.com',

      subject: 'ZIPPO Test',

      html: `
        <h2>ZIPPO Test Email</h2>
        <p>Email delivery is working.</p>
      `,
    });

    console.log(result);

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

    await this.resend.emails.send({
      from: 'ZIPPO <noreply@zippo.app>',
      to: driver.email,
      subject: 'Your ZIPPO Verification Code',
      html: `
<div style="font-family:Arial,sans-serif">

<h2>Password Reset Request</h2>

<p>Hello ${driver.name},</p>

<p>
A request was received to reset your ZIPPO account password.
</p>

<p>Your verification code is:</p>

<h1 style="color:#1976D2;">
${token}
</h1>

<p>
This code is valid for 15 minutes.
</p>

<p>
If you did not request this reset, please ignore this email.
</p>

<hr/>

<p>
Need help?
</p>

<p>
Contact: masoom117@gmail.com / (Call : +91 8121242633 )
</p>

<p>
ZIPPO Transport Management
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
