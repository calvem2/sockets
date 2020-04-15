import java.util.*;
import java.nio.ByteBuffer;
import java.net.*;
import java.io.IOException;

public class ServerMain {
    public static void main (String [] args) throws SocketException, IOException, UnknownHostException {
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
        // Stage A

        // Stage B

        // Stage C

        // Stage D
    }

    public static byte[] stageAResponse() {
        Random rand = new Random();
        // Create payload
        ByteBuffer payload = ByteBuffer.allocate(16);
        payload.putInt(rand.nextInt(500) + 1);
        payload.putInt(rand.nextInt(500) + 1);
        // UDP port number range: [12235, 65535]
        payload.putInt(rand.nextInt((65535 - ServerUtils.INIT_PORT) + 1) + ServerUtils.INIT_PORT);
        int psecret = rand.nextInt(100) + 1;
        payload.putInt(psecret);
        // Merge payload and header
        byte[] header = ServerUtils.createHeader(16, psecret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }

    public static boolean verifyRequestA(byte[] packet) {
        ByteBuffer request = ByteBuffer.wrap(packet);
        String payload = "hello world\0";
        // Verify packet header
        boolean verifyHeader = ServerUtils.verifyHeader(packet, payload.length(), 0);

        // Verify packet payload
        boolean verifyPayload = verifyHeader;
        int i = 0;
        while (verifyPayload && i < payload.length()) {
            verifyPayload = verifyPayload &&
                            payload.charAt(i) == request.getChar(i + ServerUtils.HEADER_SIZE);
            i++;
        }

        // Verify packet is padded
        boolean verifyPadding = ServerUtils.verifyPadding(packet, payload.length());
        return verifyPayload && verifyPadding;
    }


    // public static ___ stageB() {    }
    
    // public static ___ stageC() {    }
    
    // public static ___ stageD() {    }
}