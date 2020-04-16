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


    public ServerThread(byte[] stageAMessage) throws SocketException, UnknownHostException {
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
                System.out.println("Stage B returned true.");
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
                tcp.close();
            }
            System.out.println("stageB is false");
        } catch (Exception e) {
            System.out.println("Error..");
        }
    }

    // Does the server side stage B 
    public boolean stageB() throws IOException {
        int ackedPacketID = 0;
        Random rand = new Random();
        // Guarantee at least one packet is not acked
        int retransmitID = rand.nextInt(num);
        boolean retransmitted = false;
        while (ackedPacketID != num) {
            DatagramPacket packet = udp.receive(ServerUtils.HEADER_SIZE + len + 4);
            // Verify the packet from the client
            if (packet == null || !verifyStageB(packet.getData(), ackedPacketID)) {
                // TODO: delete these later
                if (packet == null) {
                    System.out.println("packet is null");
                }
                if (!verifyStageB(packet.getData(), ackedPacketID)) {
                    System.out.println("verify stage is false");
                }
                
                break;
            }
            udp.processRequest(packet);
            ByteBuffer request = ByteBuffer.wrap(packet.getData());
            // Send ACK response
            if (rand.nextBoolean()) {
                int packetID = request.getInt(ServerUtils.HEADER_SIZE);
                // Ensure at least one packet is not acked
                if (packetID != retransmitID || retransmitted) {
                    // TODO: delete this later
                    System.out.println("here is the stage B packet ID: " + ackedPacketID);
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

    // Verify the packet sent from client
    public boolean verifyStageB(byte[] packet, int id) {
        ByteBuffer request = ByteBuffer.wrap(packet);
        // Verify header
        boolean verifyHeader = ServerUtils.verifyHeader(packet, len + 4, secret);

        // TODO: delete later
        if (!verifyHeader) {
            System.out.println("Header is bad");
        }

        // Verify payload
        boolean verifyPayload = verifyHeader;
        int i = 0;
        while (verifyPayload && i < len) {
            verifyPayload = verifyPayload && (request.get(i + ServerUtils.HEADER_SIZE + 4) == 0);
            i++;
        }

        // TODO: delete later
        if (!verifyPayload) {
            System.out.println("payload isn't all 0s");
        }

        verifyPayload = verifyPayload && request.getInt(ServerUtils.HEADER_SIZE) == id;

        // TODO: delete later
        if (!verifyPayload) {
            System.out.println("payload id is bad expected: " + id + " actual: " + request.getInt(ServerUtils.HEADER_SIZE));
        }

        // Verify padding
        boolean verifyPadding = ServerUtils.verifyPadding(packet, len + 4);

        // TODO: delete later
        if (!verifyPadding) {
            System.out.println("padding is bad");
        }

        return verifyPayload && verifyPadding;
    }

    // Create the server packet sent in stage b1
    public byte[] stageB1Response(int ack) {
        ByteBuffer payload = ByteBuffer.allocate(4);
        payload.putInt(ack);
        byte[] header = ServerUtils.createHeader(4, secret);
        return ServerUtils.mergeHeaderPayload(header, payload.array());
    }

    // Create the server packet sent in stage b2
    public byte[] stageB2Response() throws UnknownHostException, IOException {
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

    // Do the server side stage C
    public void stageC() throws IOException {
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

    // Do the server size stage D
    public void stageD() throws IOException {
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

    // Verify the client's packet from stage D
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

    // Create the server packet for stage D
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