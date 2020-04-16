import java.util.*;
import java.nio.ByteBuffer;
import java.net.*;
import java.io.IOException;

public class ServerMain {
    public static void main (String [] args) {
        UDPServer udp = new UDPServer(ServerUtils.INIT_PORT, false);
        System.out.println("Socket created");
        while (true) {
            DatagramPacket dp = udp.receive(ServerUtils.HEADER_SIZE + 12);
            System.out.println("Packet received");
            if (verifyRequestA(dp.getData())) {
                udp.processRequest(dp);
                // TODO: delete this later
                System.out.println("Before the stage a response");
                byte[] response = stageAResponse();
                udp.send(response);
                ServerThread thread = new ServerThread(response);
                // TODO: delete this later 
                System.out.println("Before the thread start");
                thread.start();
            }
        }
    }

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
        // boolean verifyHeader = ServerUtils.verifyHeader(packet, payloadArr.length, 0);//payload.length, 0);
        
        // System.out.println("here is the verify header: " + verifyHeader);

        // Verify packet payload
        // boolean verifyPayload = verifyHeader;
        boolean verifyPayload = true;
        int i = 0;
        while (verifyPayload && i <  payloadArr.length) { //payload.length()) {
            verifyPayload = verifyPayload &&
                            payloadArr[i] == request.get(i + ServerUtils.HEADER_SIZE);
            i++;
        }

        // Verify packet is padded
        // boolean verifyPadding = ServerUtils.verifyPadding(packet, payloadArr.length);
        // return verifyPayload && verifyPadding;
        return verifyPayload;
    }
}