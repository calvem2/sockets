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


    public ServerThread(byte[] stageAMessage) {
        ByteBuffer response = ByteBuffer.wrap(stageAMessage);
        this.num = response.getInt(12);
        this.len = response.getInt(16);
        udp = new UDPServer(response.getInt(20), true);
        this.secret = response.getInt(24);
    }

    public void run() {
        try {
            // TODO: Remove this later
            System.out.println("Done with stage A");
            // Stage B
            // boolean stageB = stageB();
            stageB();
            udp.close();
            // TODO: Remove this later
            System.out.println("Done with stage B");

            if (tcp.accept() == null) {
                throw new Exception("TCP socket timed out waiting for connection");
            }
            stageC();
            // TODO: Remove this later
            System.out.println("Done with stage C");

            // Stage D
            stageD();
            System.out.println("Done with stage D");
            tcp.close();
            // if (stageB) {
            //     // Stage C
            //     if (tcp.accept() != null) {
            //         stageC();
            //         // TODO: Remove this later
            //         System.out.println("Done with stage C");
            //     }
            //     // Stage D
            //     stageD();
            //     // TODO: Remove this later
            //     System.out.println("Done with stage D");
            //     //tcp.close();
            // }


        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            // need to close in case we didn't bc of an exception 
        }
    }

    /// STAGE B

    public void stageB() throws Exception {
        int ackedPacketID = 0;
        Random rand = new Random();
        // Guarantee at least one packet is not acked
        int retransmitID = rand.nextInt(num);
        boolean retransmitted = false;
        while (ackedPacketID != num) {
            int expectedLength = ServerUtils.HEADER_SIZE + len + 4;
            // if (expectedLength % 4 != 0) {
            //     int padding = 4 - (expectedLength % 4);
            //     expectedLength += padding;
            // }
            expectedLength += ServerUtils.getPadding(expectedLength);
            DatagramPacket packet = udp.receive(expectedLength);
            // System.out.println("Received packet " + ByteBuffer.wrap(packet.getData()).getInt(12));
            // if (packet == null || !verifyStageB(packet.getData(), ackedPacketID)) {
            //     break;
            // }
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
                    // System.out.println("ACK " + ackedPacketID + " sent");
                    ackedPacketID++;
                }
            } else {
                retransmitted = true;
            }
        }

        // Send final response if we recieved all packets
        // if (ackedPacketID == num) {
            byte[] response = stageB2Response();
            udp.send(response);
        //     return true;
        // }
        // return false;
    }

    public void verifyStageB(byte[] packet, int id) throws Exception {
        ByteBuffer request = ByteBuffer.wrap(packet);
        // Verify packet length and header
        // boolean verifyPadding = ServerUtils.verifyPadding(packet);
        // boolean verifyHeader = ServerUtils.verifyHeader(packet, len + 4, secret);
        ServerUtils.verifyPacket(packet, len + 4, secret);

        // Verify payload
        // boolean verifyPayload = ServerUtils.VerifyPacket(packet, len + 4, secret);
        if (request.getInt(ServerUtils.HEADER_SIZE) != id) {
            System.out.println(request.getInt(ServerUtils.HEADER_SIZE));
            System.out.println(id);
            throw new Exception("Stage B packets arrived out of order");
        }
        // boolean verifyPayload = true;
        int i = 0;
        // while (verifyPayload && i < len) {
        while (i < len) {
            // verifyPayload = verifyPayload && (request.get(i + ServerUtils.HEADER_SIZE + 4) == 0);
            if (request.get(i + ServerUtils.HEADER_SIZE + 4) != 0) {
                throw new Exception("Stage B packet(s) not padded with 0s");
            }
            i++;
        }
        // verifyPayload = verifyPayload && request.getInt(ServerUtils.HEADER_SIZE) == id;
        // return verifyPayload;
    }

    public byte[] stageB1Response(int ack) {
        ByteBuffer payload = ByteBuffer.allocate(4);
        payload.putInt(ack);
        byte[] header = ServerUtils.createHeader(4, secret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }

    public byte[] stageB2Response() {
        ByteBuffer payload = ByteBuffer.allocate(8);
        Random rand = new Random();
        int tcpPort = rand.nextInt((65535 - ServerUtils.MIN_PORT) + 1) + ServerUtils.MIN_PORT;
        int psecret = rand.nextInt(100) + 1;
        tcp = new TCPServer(tcpPort);
        payload.putInt(tcpPort);
        payload.putInt(psecret);
        byte[] header = ServerUtils.createHeader(8, psecret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }

    /// STAGE C

    public void stageC() {
        ByteBuffer payload = ByteBuffer.allocate(13);
        Random rand = new Random();
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

    // STAGE D

    public void stageD() throws Exception {
        int received = 0;
        while (received != num) {
            int expectedLength = ServerUtils.HEADER_SIZE + len;
            expectedLength += ServerUtils.getPadding(expectedLength);
            byte[] request = tcp.receive(expectedLength);
            // if (request == null || !verifyStageD(request)) {
            //     break;
            // }
            if (request == null) {
                throw new Exception("Stage D timed out waiting for client");
            }
            verifyStageD(request);
            received++;
        }
        // if (received == num) {
            tcp.send(stageDResponse());
        // }
    }

    public void verifyStageD(byte[] packet) throws Exception {
        // // Verify packet header and length
        // boolean verifyHeader = ServerUtils.verifyHeader(packet, len, secret);
        // boolean verifyPadding = ServerUtils.verifyPadding(packet);

        // Verify payload
        ServerUtils.verifyPacket(packet, len, secret);
        int i = 0;
        while (i < len) {
            // verifyPayload = verifyPayload && packet[i + ServerUtils.HEADER_SIZE] == c;
            // if (verifyPayload && packet[i + ServerUtils.HEADER_SIZE] != c) {
            if (packet[i + ServerUtils.HEADER_SIZE] != c) {
                throw new Exception("Stage D packet payload incorrect");
            }
            i++;
        }

        // return verifyPayload;
    }

    public byte[] stageDResponse() {
        ByteBuffer payload = ByteBuffer.allocate(4);
        Random rand = new Random();
        int psecret = rand.nextInt(100) + 1;
        payload.putInt(psecret);

        // Merge header and payload
        byte[] header = ServerUtils.createHeader(4, psecret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }
}