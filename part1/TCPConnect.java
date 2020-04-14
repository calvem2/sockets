import java.nio.*;
import java.net.*;
import java.io.*;

public class TCPConnect {
    private static final String SERVER = "attu2.cs.washington.edu";
    
    private Socket tcp;
    private int port;

    public TCPConnect(int port) throws UnknownHostException, IOException {
        tcp = new Socket(SERVER, port);
        this.port = port;
    }

    // Send the packet to the server
    public void send(byte[] buf) throws IOException {
        OutputStream os = tcp.getOutputStream();
        os.write(buf);
    }

    // Receive a packet from the server
    public byte[] receive(int length) throws IOException {
        byte[] buf = new byte[length];
        InputStream is = tcp.getInputStream();
        int bytesRead = 0;
        while (bytesRead < length) {
            bytesRead += is.read(buf, bytesRead, (length - bytesRead));
        }

        return buf;
    }

    // Close the socket
    public void close() throws IOException {
        tcp.close();
    }
}