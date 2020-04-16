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
            boolean stageB = stageB();
            udp.close();
            // TODO: Remove this later
            System.out.println("Done with stage B");

            if (stageB) {
                // Stage C
                if (tcp.accept() != null) {
                    stageC();
                    // TODO: Remove this later
                    System.out.println("Done with stage C");
                }
                // Stage D
                stageD();
                // TODO: Remove this later
                System.out.println("Done with stage D");
                //tcp.close();
            }


        } catch (Exception e) {

        } finally {
            tcp.close();
        }
    }

    public boolean stageB() {
        int ackedPacketID = 0;
        Random rand = new Random();
        // Guarantee at least one packet is not acked
        int retransmitID = rand.nextInt(num);
        boolean retransmitted = false;
        while (ackedPacketID != num) {
            DatagramPacket packet = udp.receive(ServerUtils.HEADER_SIZE + len + 4);
            if (packet == null || !verifyStageB(packet.getData(), ackedPacketID)) {
                break;
            }
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

        // Send final response if we recieved all packets
        if (ackedPacketID == num) {
            byte[] response = stageB2Response();
            udp.send(response);
            return true;
        }
        return false;
    }

    public boolean verifyStageB(byte[] packet, int id) {
        ByteBuffer request = ByteBuffer.wrap(packet);
        // Verify header
        boolean verifyHeader = ServerUtils.verifyHeader(packet, len + 4, secret);

        // Verify payload
        boolean verifyPayload = verifyHeader;
        int i = 0;
        while (verifyPayload && i < len) {
            verifyPayload = verifyPayload && (request.get(i + ServerUtils.HEADER_SIZE + 4) == 0);
            i++;
        }
        verifyPayload = verifyPayload && request.getInt(ServerUtils.HEADER_SIZE) == id;

        // Verify padding
        boolean verifyPadding = ServerUtils.verifyPadding(packet, len + 4);

        return verifyPayload && verifyPadding;
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


    public void stageC() {
        ByteBuffer payload = ByteBuffer.allocate(13);
        Random rand = new Random();
        num = rand.nextInt(500) + 1;
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

    public void stageD() {
        int received = 0;
        while (received != num) {
            byte[] request = tcp.receive(ServerUtils.HEADER_SIZE + len);
            if (request == null || !verifyStageD(request)) {
                break;
            }
            received++;
        }
        if (received == num) {
            tcp.send(stageDResponse());
        }
    }

    public boolean verifyStageD(byte[] packet) {
        // Verify header
        boolean verifyHeader = ServerUtils.verifyHeader(packet, len, secret);

        // Verify padding
        boolean verifyPadding = ServerUtils.verifyPadding(packet, len);

        // Verify payload
        boolean verifyPayload = verifyHeader && verifyPadding;
        int i = 0;
        while (verifyPayload && i < len) {
            verifyPayload = verifyPayload && packet[i + ServerUtils.HEADER_SIZE] == c;
            i++;
        }

        return verifyPayload;
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