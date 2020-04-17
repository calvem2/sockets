import java.nio.*;
import java.net.*;
import java.io.IOException;

public class UDPServer{
    private DatagramSocket udp;
    private InetAddress clientAddr;
    private int clientPort;


    public UDPServer(int port, boolean timeout) {
        try {
            udp = new DatagramSocket(port);

            // Enable timeout
            if (timeout) {
                udp.setSoTimeout(ServerUtils.TIMEOUT);
            } 
        } catch (SocketException e) {
            System.out.println("Error: Could not open open UDP socket");
            e.printStackTrace();
        }
    }

    // Send the packet to the server
    public void send(byte[] buf) {
        try {
            DatagramPacket dp = new DatagramPacket(buf, buf.length, clientAddr, clientPort);
            udp.send(dp);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Receive a packet from the server
    public DatagramPacket receive(int length) {
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
        
        return dp;
    }

    // Process request recieved from client
    public void processRequest(DatagramPacket request) {
        clientAddr = request.getAddress();
        clientPort = request.getPort();
    }

    // Close the socket
    public void close() {
        udp.close();
    }
}