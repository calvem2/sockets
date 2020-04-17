import java.util.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.io.IOException;

public class ServerThread extends Thread {
    private UDPServer udp;
    private TCPServer tcp;
    private int num;
    private int len;
    private byte c;
    private int secret;

    public ServerThread(byte[] stageAMessage, UDPServer server) {
        ByteBuffer response = ByteBuffer.wrap(stageAMessage);
        this.num = response.getInt(12);
        this.len = response.getInt(16);
        udp = server;
        this.secret = response.getInt(24);
    }

    public void run() {
        try {
            // Stage B
            stageB();
            udp.close();

            // If the TCP socket timed out
            if (tcp.accept() == null) {
                throw new Exception("TCP socket timed out waiting for connection");
            }
            // Stage C
            stageC();

            // Stage D
            stageD();
            tcp.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            // Clean up
            udp.close();
            tcp.close();
        }
    }

    // Executes Stage B:
    // Receive client's Stage B packets, send ACKs, and
    // send final response after all packets have been received
    public void stageB() throws Exception {
        int ackedPacketID = 0;
        Random rand = new Random();
        // Guarantee at least one packet is not acked
        int retransmitID = rand.nextInt(num);
        boolean retransmitted = false;
        while (ackedPacketID != num) {
            int expectedLength = ServerUtils.HEADER_SIZE + len + 4;
            expectedLength += ServerUtils.getPadding(expectedLength);
            DatagramPacket packet = udp.receive(expectedLength);
            // If no packet was received in the allotted time
            if (packet == null) {
                throw new Exception("Stage B timed out waiting for client");
            }
            verifyStageB(packet.getData(), ackedPacketID);
            udp.processRequest(packet);
            ByteBuffer request = ByteBuffer.wrap(packet.getData());
            // Send ACK response
            if (rand.nextBoolean()) {
                int packetID = request.getInt(ServerUtils.HEADER_SIZE);
                // Ensure at least one packet is not acked
                if (packetID != retransmitID || retransmitted) {
                    udp.send(stageB1Response(ackedPacketID));
                    ackedPacketID++;
                }
            } else {
                retransmitted = true;
            }
        }

        // Send final response if we received all packets
        byte[] response = stageB2Response();
        udp.send(response);
    }

    // Verify the client packet received from Stage B
    public void verifyStageB(byte[] packet, int id) throws Exception {
        ByteBuffer request = ByteBuffer.wrap(packet);
        // Verify the packet
        ServerUtils.verifyPacket(packet, len + 4, secret);

        // Verify payload
        if (request.getInt(ServerUtils.HEADER_SIZE) != id) {
            throw new Exception("Stage B packets arrived out of order");
        }
        int i = 0;
        while (i < len) {
            if (request.get(i + ServerUtils.HEADER_SIZE + 4) != 0) {
                throw new Exception("Stage B packet(s) not padded with 0s");
            }
            i++;
        }
    }

    // Create the server packet sent in first part of Stage B (ACK packets)
    public byte[] stageB1Response(int ack) {
        ByteBuffer payload = ByteBuffer.allocate(4);
        payload.putInt(ack);
        byte[] header = ServerUtils.createHeader(4, secret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }

    // Create the server packet sent in second part of Stage B
    public byte[] stageB2Response() {
        ByteBuffer payload = ByteBuffer.allocate(8);
        Random rand = new Random();
        // Generate payload
        int psecret = rand.nextInt(100) + 1;
        tcp = new TCPServer(0);
        payload.putInt(tcp.getLocalPort());
        payload.putInt(psecret);
        // Merge payload with header
        byte[] header = ServerUtils.createHeader(8, psecret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }

    // Executes Stage C
    public void stageC() {
        ByteBuffer payload = ByteBuffer.allocate(13);
        Random rand = new Random();
        // Generate payload values for response packet
        num = rand.nextInt(100) + 1;
        len = rand.nextInt(500) + 1;
        secret = rand.nextInt(100) + 1;
        c = (byte) (rand.nextInt(94) + 33);
        // Populate payload
        payload.putInt(num);
        payload.putInt(len);
        payload.putInt(secret);
        payload.put(c);
        // Merge payload and header
        byte[] header = ServerUtils.createHeader(12, secret);
        byte[] response = ServerUtils.mergeHeaderPayload(header, payload.array());
        tcp.send(response);
    }

    // Executes Stage D:
    // Receive all packets from client then send final response packet
    public void stageD() throws Exception {
        int received = 0;
        while (received != num) {
            int expectedLength = ServerUtils.HEADER_SIZE + len;
            expectedLength += ServerUtils.getPadding(expectedLength);
            byte[] request = tcp.receive(expectedLength);
            // If no packet was received in the allotted time
            if (request == null) {
                throw new Exception("Stage D timed out waiting for client");
            }
            verifyStageD(request);
            received++;
        }
        tcp.send(stageDResponse());
    }

    // Verify packets received from client in Stage D
    public void verifyStageD(byte[] packet) throws Exception {
        // Verify packet header and length
        ServerUtils.verifyPacket(packet, len, secret);
        // Verify payload
        int i = 0;
        while (i < len) {
            if (packet[i + ServerUtils.HEADER_SIZE] != c) {
                throw new Exception("Stage D packet payload incorrect");
            }
            i++;
        }
    }

    // Create the server packet sent in stage D
    public byte[] stageDResponse() {
        // Create the payload
        ByteBuffer payload = ByteBuffer.allocate(4);
        Random rand = new Random();
        int psecret = rand.nextInt(100) + 1;
        payload.putInt(psecret);

        // Merge header and payload
        byte[] header = ServerUtils.createHeader(4, psecret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }
}