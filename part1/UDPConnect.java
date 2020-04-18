import java.nio.*;
import java.net.*;
import java.io.IOException;

public class UDPConnect {    
    private DatagramSocket udp;
    private int port;
    private String server;

    public UDPConnect(int port, String serverName) {
        this(port, serverName, 0);
    }

    public UDPConnect(int port, String serverName, int timeout) {
        try {
            udp = new DatagramSocket();
            this.port = port;
            this.server = serverName;
            // Connect to the server
            InetAddress addr = InetAddress.getByName(server);
            udp.connect(addr, port);

            // Enable timeout
            if (timeout > 0) {
                udp.setSoTimeout(timeout);
            }
        } catch (SocketException e) {
            System.out.println("Error: Could not open open UDP socket");
            e.printStackTrace();
            System.exit(0);
        } catch (UnknownHostException e) {
            System.out.println("Error: Could not find host");
            e.printStackTrace();
            System.exit(0);
        }
    }

    // Send the packet to the server
    public void send(byte[] buf) {
        try {
            InetAddress addr = InetAddress.getByName(server);
            DatagramPacket dp = new DatagramPacket(buf, buf.length, addr, port);
            udp.send(dp);
        } catch (UnknownHostException e) {
            System.out.println("Error: Could not find host");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Receive a packet from the server
    public byte[] receive(int length) {
        byte[] buf = new byte[length];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);

        try {
            udp.receive(dp);
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return buf;
    }

    // Close the socket
    public void close() {
        udp.close();
    }
}