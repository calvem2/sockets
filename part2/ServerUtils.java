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
        // int padding = 0;
        // // If the packet needs to be padded
        // if ((header.length + payload.length) % 4 != 0) {
        //     padding = 4 - ((header.length + payload.length) % 4);
        // }
        int padding = getPadding(header.length + payload.length);
        // Create new ByteBuffer to put header and payload in with necessary padding
        ByteBuffer mergedBuf = ByteBuffer.allocate(header.length + payload.length + padding);
        mergedBuf.put(header);
        mergedBuf.put(payload);

        return mergedBuf.array();
    }

    public static int getPadding(int len) {
        int padding = 0;
        if (len % 4 != 0) {
            padding += 4 - (len % 4);
        }
        return padding;
    }

    public static void verifyPacket(byte[] packet, int len, int secret) throws Exception {
        ByteBuffer request = ByteBuffer.wrap(packet);
        // int padding = 0;
        // if ((HEADER_SIZE + len) % 4 != 0) {
        //     padding = 4 - ((HEADER_SIZE + len) % 4);
        // }
        int padding = getPadding(HEADER_SIZE + len);
        if (packet.length != len + padding + HEADER_SIZE) {
            // System.out.println(packet.length);
            // System.out.println(len + padding + HEADER_SIZE);
            throw new Exception("Packet not aligned correctly");
        } else if (request.getInt(0) != len ||
                   request.getInt(4) != secret ||
                   request.getShort(8) != 1 ||
                   request.getShort(10) != ServerUtils.SID_NUM) {
            throw new Exception("Packet header incorrect");
        }
        // System.out.println("Packet verified");
        // return packet.length % 4 == 0 &&
        //        request.getInt(0) == len &&
        //        request.getInt(4) == secret &&
        //        request.getShort(8) == 1 &&
        //        request.getShort(10) == ServerUtils.SID_NUM;
    }

//     public static boolean verifyPadding(byte[] packet) {
//         // Verify packet is 4-byte aligned
//         // if (packet.length % 4 != 0) {
//         //     return false;
//         // }
//         // Verify packet is padded with 0's
// //        for (int i = payloadLen + HEADER_SIZE; i < packet.length; i++) {
// //            if (packet[i] != 0) {
// //                return false;
// //            }
// //        }
//         // return true;
//         return packet.length % 4 == 0
//     }
}