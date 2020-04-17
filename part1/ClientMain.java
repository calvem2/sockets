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
    
    public static void main (String[] args) {
        // Stage A
        byte[] aResponsePacket = stageA();
        
        // TODO: Remove later
        System.out.println("Done with stage A");
        
        // Stage B
        byte[] bResponsePacket = stageB(aResponsePacket);
        
        // TODO: Remove later
        System.out.println("Done with stage B");

        // Stage C
        byte[] cResponsePacket = stageC(bResponsePacket);
        
        // TODO: Remove later
        System.out.println("Done with stage C");

        // Stage D
        byte[] dResponsePacket = stageD(cResponsePacket);
        
        // TODO: Remove later
        System.out.println("Done with stage D");
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
    public static byte[] stageA() {
        UDPConnect udp = new UDPConnect(PORT);
        // Create the payload, header, and packet
        String str = "hello world\0";
        byte[] payload = str.getBytes();       
        byte[] header = createHeader(payload.length, 0);
        byte[] packet = mergeHeaderPayload(header, payload);
        // Send the packet and receive a packet from the server
        udp.send(packet);
        // System.out.println("Packet sent");
        byte[] response = udp.receive(HEADER_SIZE + 16);
        // Close the UDP socket
        udp.close();

        return response;
    }

    // Does the client side stage B
    public static byte[] stageB(byte[] packet) {
        // Creates the byte buffer from the packet received
        // from the server from stage A
        ByteBuffer packetBuf = ByteBuffer.allocate(packet.length);
        packetBuf.put(packet);

        // Extracts the information from the server packet
        int numPackets = packetBuf.getInt(12);
        int length = packetBuf.getInt(16);
        int udp_port = packetBuf.getInt(20);
        int secretA = packetBuf.getInt(24);
        // Create a new udp connection
        UDPConnect udp = new UDPConnect(udp_port, 500);
        // Create the header for the packets
        byte[] header = createHeader(length + 4, secretA);

        // Transmit the number of UDP packets that the 
        // server specifies
        for (int i = 0; i < numPackets; i++) {
            // Create the payload for the given packet
            ByteBuffer payload = ByteBuffer.allocate(length + 4);
            payload.putInt(i);
            // Merge the header and packet
            byte[] dataPacket = mergeHeaderPayload(header, payload.array());

            byte[] ackedPacket = null;
            // Send the packet until the correct corresponding 
            // ack packet has been received from the server
            while (ackedPacket == null || ByteBuffer.wrap(ackedPacket).getInt(12) != i) {
                udp.send(dataPacket);
                ackedPacket = udp.receive(HEADER_SIZE + 4);
            }
        }

        byte[] response = udp.receive(HEADER_SIZE + 8);
        // Close the UDP socket
        udp.close();

        return response;
    }

    // Does the client side stage C
    public static byte[] stageC(byte[] packet) {
        // Creates a byte buffer from the packet received
        // from the server in stage B
        ByteBuffer packetBuf = ByteBuffer.allocate(packet.length);
        packetBuf.put(packet);
        // Get the information from the server packet
        int tcpPort = packetBuf.getInt(12);
        // Connect to the TCP port received from
        // the server
        tcp = new TCPConnect(tcpPort);
        // Receive the packet from the server
        byte[] response = tcp.receive(HEADER_SIZE + 16);

        return response;
    }

    // Does the client side stage D
    public static byte[] stageD(byte[] packet) {
        // Creates a byte buffer from the packet received
        // from the server in stage C
        ByteBuffer packetBuf = ByteBuffer.allocate(packet.length);
        packetBuf.put(packet);
        // Get the information from the server packet
        int numPackets = packetBuf.getInt(12);
        int length = packetBuf.getInt(16);
        int secretC = packetBuf.getInt(20);
        byte c = packetBuf.get(24);
        // Create the header
        byte[] header = createHeader(length, secretC);
        // Create the payload for the packet
        byte[] payload = new byte[length]; 
        for (int i = 0; i < payload.length; i++) {
            payload[i] = c;
        }
        // Merge the header and payload into one packet
        byte[] dataPacket = mergeHeaderPayload(header, payload);
        // Send the number of packets specified by the server
        for (int i = 0; i < numPackets; i++) {
            tcp.send(dataPacket);
        }
        // Receive the packet from the server
        byte[] response = tcp.receive(HEADER_SIZE + 4);
        tcp.close();

        return response;
    }
}