import java.nio.*;
import java.net.*;
import java.io.IOException;

public class UDPConnect {
    private static final String SERVER = "attu6.cs.washington.edu";
    
    private DatagramSocket udp;
    private int port;

    public UDPConnect(int port) throws SocketException, UnknownHostException {
        this(port, 0);
    }

    public UDPConnect(int port, int timeout) throws SocketException, UnknownHostException {
        udp = new DatagramSocket();
        this.port = port;
        // Connect to the server
        InetAddress addr = InetAddress.getByName(SERVER);
        udp.connect(addr, port);

        // Enable timeout
        if (timeout > 0) {
            udp.setSoTimeout(timeout);
        }  
    }

    // Send the packet to the server
    public void send(byte[] buf) throws UnknownHostException, IOException {
        InetAddress addr = InetAddress.getByName(SERVER);
        DatagramPacket dp = new DatagramPacket(buf, buf.length, addr, port);
        udp.send(dp);
    }

    // Receive a packet from the server
    public byte[] receive(int length) throws IOException {
        byte[] buf = new byte[length];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);

        try {
            udp.receive(dp);
        } catch (SocketTimeoutException e) {
            return null;
        }
        
        return buf;
    }

    // Close the socket
    public void close() {
        udp.close();
    }
}