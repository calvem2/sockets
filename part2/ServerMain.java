import java.util.*;
import java.nio.ByteBuffer;
import java.net.*;
import java.io.IOException;

public class ServerMain {
    public static void main (String [] args) {
        UDPServer udp = new UDPServer(ServerUtils.INIT_PORT, false);
        // Start receiving from clients
        while (true) {
            DatagramPacket dp = udp.receive(ServerUtils.HEADER_SIZE + 12);
            // If request is valid, start a new thread to handle the rest
            // of the client's stages
            if (verifyRequestA(dp.getData())) {
                udp.processRequest(dp);                
                // Get UDPServer and its port to be used in Stage B
                UDPServer stageBServer = new UDPServer(0, true);
                int stageBPort = stageBServer.getLocalPort();
                // Put UDP port into Stage A response packet
                byte[] response = ByteBuffer.wrap(stageAResponse()).putInt(20, stageBPort).array();
                udp.send(response);
                ServerThread thread = new ServerThread(response, stageBServer); 
                thread.start();
            }
        }
    }

    /*
     * Create response packet for Stage A
     */
    public static byte[] stageAResponse() {
        Random rand = new Random();
        // Create payload: [num, len, udp port, secret]
        ByteBuffer payload = ByteBuffer.allocate(16);
        payload.putInt(rand.nextInt(100) + 1);
        payload.putInt(rand.nextInt(500) + 1);
        int psecret = rand.nextInt(100) + 1;
        payload.putInt(12, psecret);
        // Merge payload with a packet header
        byte[] header = ServerUtils.createHeader(16, psecret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }

    /*
     * Verify the client request for stage A
     */
    public static boolean verifyRequestA(byte[] packet) {
        ByteBuffer request = ByteBuffer.wrap(packet);
        String payload = "hello world\0";
        byte[] payloadArr = payload.getBytes(); 
        // Verify packet header and length
        try {
            ServerUtils.verifyPacket(packet, payloadArr.length, 0);
        } catch (Exception e) {
            return false;
        }
        // Verify packet payload
        boolean verifyPayload = true;
        int i = 0;
        while (verifyPayload && i <  payloadArr.length) {
            verifyPayload = verifyPayload &&
                            payloadArr[i] == request.get(i + ServerUtils.HEADER_SIZE);
            i++;
        }
        return verifyPayload;
    }
}