export class CreateDriverDto {
  name: string;
  phoneNumber: string;
  email: string;
  vehicleNumber?: string;
  role?: string;
  password: string;
}