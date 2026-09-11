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
