import java.util.*;
import java.nio.ByteBuffer;
import java.net.*;
import java.io.IOException;

public class ClientMain {
    public static final int PORT = 12235;
    public static final short STEP = 1;
    public static final int HEADER_SIZE = 12;
    public static final short SID_NUM = 947;

    public static TCPConnect tcp;
    
    public static void main (String[] args) throws SocketException, UnknownHostException, IOException {
        // Stage A
        byte[] aResponsePacket = stageA();
        // TODO: Remove later
        System.out.println("Done with stage A");
        System.out.println(Arrays.toString(aResponsePacket));
        
        // Stage B
        byte[] bResponsePacket = stageB(aResponsePacket);
        // TODO: Remove later
        System.out.println("Done with stage B");
        System.out.println(Arrays.toString(bResponsePacket));

        // Stage C
        byte[] cResponsePacket = stageC(bResponsePacket);
        // TODO: Remove later
        System.out.println("Done with stage C");
        System.out.println(Arrays.toString(cResponsePacket));

        // Stage D
        byte[] dResponsePacket = stageD(cResponsePacket);
        // TODO: Remove later
        System.out.println("Done with stage D");
        System.out.println(Arrays.toString(dResponsePacket));
    }

    // Create the header for the packets
    public static byte[] createHeader(int payloadLen, int pSecret) {
        ByteBuffer byteBuf = ByteBuffer.allocate(HEADER_SIZE);
        byteBuf.putInt(payloadLen);
        byteBuf.putInt(pSecret);
        byteBuf.putShort(STEP);
        byteBuf.putShort(SID_NUM);
        
        return byteBuf.array();
    }

    // Merge the header and the payload
    // Return the array of bytes with the header and payload merged together
    public static byte[] mergeHeaderPayload(byte[] header, byte[] payload) {
        int padding = 0;
        // If the packet needs to be padded
        if ((header.length + payload.length) % 4 != 0) {
            padding = 4 - ((header.length + payload.length) % 4);
        } 
        // Create new ByteBuffer to put header and payload in with necessary padding
        ByteBuffer mergedBuf = ByteBuffer.allocate(header.length + payload.length + padding);
        mergedBuf.put(header);
        mergedBuf.put(payload);

        return mergedBuf.array();
    }

    // Does the client side stage A
    public static byte[] stageA() throws SocketException, UnknownHostException, IOException {
        UDPConnect udp = new UDPConnect(PORT);
        // Create the payload, header, and packet
        String str = "hello world\0";
        byte[] payload = str.getBytes();       
        byte[] header = createHeader(payload.length, 0);
        byte[] packet = mergeHeaderPayload(header, payload);
        // Send the packet and receive a packet from the server
        udp.send(packet);
        byte[] response = udp.receive(HEADER_SIZE + 32);
        // Close the UDP socket
        udp.close();

        return response;
    }

    // Does the client side stage B
    public static byte[] stageB(byte[] packet) throws UnknownHostException, IOException {
        ByteBuffer packetBuf = ByteBuffer.allocate(packet.length);
        packetBuf.put(packet);

        int numPackets = packetBuf.getInt(12);
        int length = packetBuf.getInt(16);
        int udp_port = packetBuf.getInt(20);
        int secretA = packetBuf.getInt(24);
        UDPConnect udp = new UDPConnect(udp_port, 500);
        byte[] header = createHeader(length + 4, secretA);

        //
        for (int i = 0; i < numPackets; i++) {
            ByteBuffer payload = ByteBuffer.allocate(length + 4);
            payload.putInt(i);
            byte[] dataPacket = mergeHeaderPayload(header, payload.array());

            byte[] ackedPacket = null;
            while (ackedPacket == null || ByteBuffer.wrap(ackedPacket).getInt(12) != i) {
                udp.send(dataPacket);
                ackedPacket = udp.receive(HEADER_SIZE + 4);
            }
        }

        // TODO: remove this later
        System.out.println(secretA);

        byte[] response = udp.receive(HEADER_SIZE + 16);
        // Close the UDP socket
        udp.close();

        return response;
    }

    //
    public static byte[] stageC(byte[] packet) throws UnknownHostException, IOException  {
        ByteBuffer packetBuf = ByteBuffer.allocate(packet.length);
        packetBuf.put(packet);

        int tcpPort = packetBuf.getInt(12);
        int secretB = packetBuf.getInt(16);
        
        tcp = new TCPConnect(tcpPort);
        byte[] response = tcp.receive(HEADER_SIZE + 13);

        return response;
    }

    //
    public static byte[] stageD(byte[] packet) throws IOException {
        ByteBuffer packetBuf = ByteBuffer.allocate(packet.length);
        packetBuf.put(packet);

        int numPackets = packetBuf.getInt(12);
        int length = packetBuf.getInt(16);
        int secretC = packetBuf.getInt(20);
        byte c = packetBuf.get(24);
        byte[] header = createHeader(length, secretC);
        byte[] payload = new byte[length]; 
        for (int i = 0; i < payload.length; i++) {
            payload[i] = c;
        }

        byte[] dataPacket = mergeHeaderPayload(header, payload);
        for (int i = 0; i < numPackets; i++) {
            tcp.send(dataPacket);
        }
        
        byte[] response = tcp.receive(HEADER_SIZE + 4);
        tcp.close();

        return response;
    }
}