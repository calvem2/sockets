import java.nio.*;
import java.net.*;
import java.io.IOException;

public class UDPServer{
    // Server socket
    private DatagramSocket udp;
    private InetAddress clientAddr;
    private int clientPort;


    public UDPServer(int port, boolean timeout) throws SocketException, UnknownHostException {
        udp = new DatagramSocket(port);

        // Enable timeout
        if (timeout) {
            udp.setSoTimeout(ServerUtils.TIMEOUT);
        }  
    }

    // Send the packet to the server
    public void send(byte[] buf) throws UnknownHostException, IOException {
        DatagramPacket dp = new DatagramPacket(buf, buf.length, clientAddr, clientPort);
        udp.send(dp);
    }

    // Receive a packet from the server
    public DatagramPacket receive(int length) throws IOException {
        byte[] buf = new byte[length];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);

        try {
            udp.receive(dp);
        } catch (SocketTimeoutException e) {
            return null;
        }
        
        return dp;
    }

    // Process request recieved from client
    public byte[] processRequest(DatagramPacket request) {
        clientAddr = request.getAddress();
        clientPort = request.getPort();
        byte[] data = request.getData();
        return data;
    }

    // Close the socket
    public void close() {
        udp.close();
    }
}