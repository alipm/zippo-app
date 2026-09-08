import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';

@Module({
  imports: [
    // Replace the string below with your exact MongoDB connection string from your Atlas account
    MongooseModule.forRoot('mongodb+srv://YOUR_USERNAME:YOUR_PASSWORD@cluster0.mongodb.net/zippo_db?retryWrites=true&w=majority'),
  ],
})
export class AppModule {}
