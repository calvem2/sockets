import java.nio.*;
import java.net.*;
import java.io.*;

public class TCPServer {    
    private ServerSocket tcp;
    private Socket socket;

    /*
     * Creates new TCPServer with tcp socket
     * bound to the given port with timeout enabled
     */
    public TCPServer(int port)  {
        try {
            tcp = new ServerSocket(port);
            tcp.setSoTimeout(ServerUtils.TIMEOUT);
        } catch (IOException e) {
            System.out.println("Error: Could not open socket");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /*
     * Return the local port this TCPServer's
     * tcp socket is bound to
     */
    public int getLocalPort() {
        return tcp.getLocalPort();
    }

    /*
     * Accept a connection on the tcp socket.
     */
    public Socket accept() {
        try {
            socket = tcp.accept();
            socket.setSoTimeout(ServerUtils.TIMEOUT);
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return socket;
    }

    /*
     * Send given buf from this tcp socket
     */
    public void send(byte[] buf) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write(buf);
        } catch (IOException e) {
            System.out.println("Error: Could not send packet");
            e.printStackTrace();
        }
    }

    /*
     * Receive packet of given length on this socket
     */
    public byte[] receive(int length) {
        byte[] buf = new byte[length];
        try {
            InputStream is = socket.getInputStream();
            int bytesRead = 0;
            while (bytesRead < length) {
                bytesRead += is.read(buf, bytesRead, (length - bytesRead));
            }
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.out.println("Error: Could not receive packet");
            e.printStackTrace();
        }
        return buf;
    }


    /*
     * Close the socket
     */
    public void close() {
        try {
            if (!tcp.isClosed()) {
            tcp.close();
            }
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error: Could not close socket");
            e.printStackTrace();
        }
    }
}