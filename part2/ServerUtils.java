import java.nio.ByteBuffer;

public class ServerUtils {
    public static final short STEP = 2;
    public static final short SID_NUM = 947;
    public static final int INIT_PORT = 12235;
    public static final int MIN_PORT = 12236;
    public static final int HEADER_SIZE = 12;
    public static final int TIMEOUT = 3000;

    // Create the header for the packets
    public static byte[] createHeader(int payloadLen, int pSecret) {
        ByteBuffer byteBuf = ByteBuffer.allocate(HEADER_SIZE);
        byteBuf.putInt(payloadLen);
        byteBuf.putInt(pSecret);
        byteBuf.putShort(STEP);
        byteBuf.putShort(SID_NUM);

        return byteBuf.array();
    }

    // Merge the header and the payload
    // Return the array of bytes with the header and payload merged together
    public static byte[] mergeHeaderPayload(byte[] header, byte[] payload) {
        // Get the amount of padding needed
        int padding = getPadding(header.length + payload.length);
        // Create new ByteBuffer to put header and payload in with necessary padding
        ByteBuffer mergedBuf = ByteBuffer.allocate(header.length + payload.length + padding);
        mergedBuf.put(header);
        mergedBuf.put(payload);

        return mergedBuf.array();
    }

    // Return the amount of padding needed
    public static int getPadding(int len) {
        int padding = 0;
        if (len % 4 != 0) {
            padding += 4 - (len % 4);
        }
        return padding;
    }

    // Verify the packet received from the client
    public static void verifyPacket(byte[] packet, int len, int secret) throws Exception {
        ByteBuffer request = ByteBuffer.wrap(packet);
        int padding = getPadding(HEADER_SIZE + len);
        // Throw an exception if the packet is not aligned right or the
        // header is is incorrect
        if (packet.length != len + padding + HEADER_SIZE) {
            throw new Exception("Packet not aligned correctly");
        } else if (request.getInt(0) != len ||
                   request.getInt(4) != secret ||
                   request.getShort(8) != 1 ||
                   request.getShort(10) != ServerUtils.SID_NUM) {
            throw new Exception("Packet header incorrect");
        }
    }
}