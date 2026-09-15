import { 
  WebSocketGateway, 
  WebSocketServer, 
  SubscribeMessage, 
  MessageBody, 
  ConnectedSocket 
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';

// Opens up a persistent duplex WebSocket connection channel
@WebSocketGateway({
  cors: { origin: '*' }, // Allows your mobile emulator network layers to bridge cleanly
  namespace: 'tracking'
})
export class TrackingGateway {
  @WebSocketServer()
  server: Server;

  // 1. LISTEN FOR LIVE DRIVER LOCATION BROADCASTS FROM THE APP
  @SubscribeMessage('update-location')
  handleLocationUpdate(
    @MessageBody() data: { driverId: string; lat: number; lng: number; speed?: number },
    @ConnectedSocket() client: Socket
  ) {
    console.log(`📡 [Live GPS] Driver ${data.driverId} is at (${data.lat}, ${data.lng})`);

    // 🟢 ADD: Simulated Target Node for Rohan Kumar's Family Pickup (Nacharam Staging Gate)
    const targetPassengerLat = 17.4421;
    const targetPassengerLng = 78.5645;
    
    // Calculates distance vectors in meters natively on the server layer using the Haversine method
    const distanceInMeters = this.calculateServerHaversine(data.lat, data.lng, targetPassengerLat, targetPassengerLng);

    // 🟢 AUTOMATED CLOUD TRIGGER: If the vehicle breaks the 100m geofence bound, trigger out-of-band dispatch alerts
    if (distanceInMeters <= 100.0) {
      console.log(`🚨 [GEOFENCE BREACH] Driver is within ${Math.round(distanceInMeters)}m of Rohan Kumar's Gate!`);
      this.triggerAutomatedOutboundAlert('9849011223', `Zippo Auto-Alert: Driver is arriving in less than 2 minutes! Please step out to the pickup point.`);
    }

    // 2. REFLECT & ROOM BROADCAST STRAIGHT TO MONITORING SUPERVISORS
    // Room partitioning prevents route tracking crosstalk across separate vehicles
    const roomName = `supervisor-room-${data.driverId}`;
    this.server.to(roomName).emit('live-location-feed', {
      timestamp: new Date().toISOString(),
      lat: data.lat,
      lng: data.lng,
      speed: data.speed || 0
    });

    return { status: 'acknowledged' };
  }

  // 🟢 ADD: High precision mathematical Haversine algorithm helper on the server
  private calculateServerHaversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371000; // Earth radius constant mapped in meters
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = 
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  // 🟢 ADD: Background gateway communicator stub (Will link to Twilio / WhatsApp cloud API endpoints in Phase 2)
  private triggerAutomatedOutboundAlert(phoneNumber: string, message: string) {
    console.log(`📱 [Automated Cloud Notification Channel] Message dispatched to ${phoneNumber}: "${message}"`);
    // Outbound Cloud HTTP POST integration pipe logic goes here...
  }
  
  // 3. HANDLES SUPERVISOR MONITORING CONNECTION JOIN TARGETS
  @SubscribeMessage('join-monitoring')
  handleJoinRoom(
    @MessageBody() data: { driverId: string },
    @ConnectedSocket() client: Socket
  ) {
    const roomName = `supervisor-room-${data.driverId}`;
    client.join(roomName);
    console.log(`👁️ [Supervisor] Connected to live monitoring stream for Driver: ${data.driverId}`);
    return { status: 'joined', room: roomName };
  }
}
