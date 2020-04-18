import java.nio.*;
import java.net.*;
import java.io.*;

public class TCPConnect {    
    private Socket tcp;
    private int port;

    public TCPConnect(int port, String server) {
        try {
            tcp = new Socket(server, port);
            this.port = port;
        } catch (UnknownHostException e) {
            System.out.println("Error: Could not find host");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error: Could not create socket");
            e.printStackTrace();
        }
    }

    // Send the packet to the server
    public void send(byte[] buf) {
        try {
            OutputStream os = tcp.getOutputStream();
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
            InputStream is = tcp.getInputStream();
            int bytesRead = 0;
            while (bytesRead < length) {
                bytesRead += is.read(buf, bytesRead, (length - bytesRead));
            }
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
        } catch (IOException e) {
            System.out.println("Error: Could not close socket");
            e.printStackTrace();
        }
    }
}