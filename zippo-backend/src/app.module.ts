// zippo-backend/src/app.module.ts
import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';

@Module({
  imports: [
    // 1. Initialize the global environment configuration controller
    ConfigModule.forRoot({
      isGlobal: true, // Makes environmental variables accessible across all application modules
    }),

    // 2. Connect dynamically to your MongoDB Atlas cloud database instance
    MongooseModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: async (configService: ConfigService) => ({
        uri: configService.get<string>('DATABASE_URL'),
      }),
      inject: [ConfigService],
    }),
  ],
})

export class AppModule {}
