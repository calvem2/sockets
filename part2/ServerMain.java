import java.util.*;
import java.nio.ByteBuffer;
import java.net.*;
import java.io.IOException;

public class ServerMain {
    public static void main (String [] args) {
        UDPServer udp = new UDPServer(ServerUtils.INIT_PORT, false);
        while (true) {
            DatagramPacket dp = udp.receive(ServerUtils.HEADER_SIZE + 12);
            if (verifyRequestA(dp.getData())) {
                udp.processRequest(dp);                
                byte[] response = stageAResponse();
                udp.send(response);
                ServerThread thread = new ServerThread(response); 
                thread.start();
            }
        }
    }

    // Does the server side for stage A
    public static byte[] stageAResponse() {
        Random rand = new Random();
        // Create payload
        ByteBuffer payload = ByteBuffer.allocate(16);
        payload.putInt(rand.nextInt(100) + 1);
        payload.putInt(rand.nextInt(500) + 1);
        // UDP port number range: [12235, 65535]
        payload.putInt(rand.nextInt((65535 - ServerUtils.MIN_PORT) + 1) + ServerUtils.MIN_PORT);
        int psecret = rand.nextInt(100) + 1;
        payload.putInt(psecret);
        // Merge payload and header
        byte[] header = ServerUtils.createHeader(16, psecret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }

    // Verify the client request for stage A
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

        // Verify packet is padded
        return verifyPayload;
    }
}