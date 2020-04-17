import java.nio.*;
import java.net.*;
import java.io.*;

public class TCPServer {    
    private ServerSocket tcp;
    private Socket socket;

    public TCPServer(int port)  {
        try {
            tcp = new ServerSocket(port);
            tcp.setSoTimeout(ServerUtils.TIMEOUT);
        } catch (IOException e) {
            System.out.println("Error: Could not open socket");
            e.printStackTrace();
        }
    }

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

    // Send the packet to the server
    public void send(byte[] buf) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write(buf);
        } catch (IOException e) {
            System.out.println("Error: Could not send packet");
            e.printStackTrace();
        }
    }

    // Receive a packet from the server
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

    // Close the socket
    public void close() {
        try {
            tcp.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Error: Could not close socket");
            e.printStackTrace();
        }
    }
}