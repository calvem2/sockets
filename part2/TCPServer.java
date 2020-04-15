import java.nio.*;
import java.net.*;
import java.io.*;

public class TCPServer {    
    private ServerSocket tcp;
    private Socket socket;
//    private int port;

    public TCPServer(int port) throws UnknownHostException, IOException {
        tcp = new ServerSocket(port);
        tcp.setSoTimeout(ServerUtils.TIMEOUT);
    }

    public Socket accept() throws IOException, SocketException {
        try {
            socket = tcp.accept();
            socket.setSoTimeout(ServerUtils.TIMEOUT);
            return socket;
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    // Send the packet to the server
    public void send(byte[] buf) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(buf);
    }

    // Receive a packet from the server
    public byte[] receive(int length) throws IOException {
        try {
            byte[] buf = new byte[length];
            InputStream is = socket.getInputStream();
            int bytesRead = 0;
            while (bytesRead < length) {
                bytesRead += is.read(buf, bytesRead, (length - bytesRead));
            }
            return buf;
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    // Close the socket
    public void close() throws IOException {
        tcp.close();
        socket.close();
    }
}