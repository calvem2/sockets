import java.nio.*;
import java.net.*;
import java.io.IOException;

public class UDPServer{
    private DatagramSocket udp;
    private InetAddress clientAddr;
    private int clientPort;

    /*
     * Creates new UDPServer with its socket
     * bound to the given port. If timeout is true,
     * enable timeout for socket.
     */
    public UDPServer(int port, boolean timeout) {
        try {
            if (port == 0) {
                udp = new DatagramSocket();
            } else {
                udp = new DatagramSocket(port);
            }

            // Enable timeout
            if (timeout) {
                udp.setSoTimeout(ServerUtils.TIMEOUT);
            } 
        } catch (SocketException e) {
            System.out.println("Error: Could not open open UDP socket");
            e.printStackTrace();
        }
    }

    /*
     * return the local port this UDPServer's socket is bound to
     */
    public int getLocalPort() {
        return udp.getLocalPort();
    }

    /*
     * Send given buf from this socket
     */
    public void send(byte[] buf) {
        try {
            DatagramPacket dp = new DatagramPacket(buf, buf.length, clientAddr, clientPort);
            udp.send(dp);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * Receive packet of given length on this socket
     */
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

    /*
     * Set this UDPServer's client address and port
     * using info from recently received DatagramPacket request.
     * Call after receiving and before sending from this socket
     */
    public void processRequest(DatagramPacket request) {
        clientAddr = request.getAddress();
        clientPort = request.getPort();
    }

    /*
     * Close the socket
     */
    public void close() {
        if (!udp.isClosed()) {
            udp.close();
        }
    }
}