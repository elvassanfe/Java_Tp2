import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Tests UDP datagram size limits over IPv4 (Section 2, Step 6).
 * Max payload = 65535 (max IP packet) - 20 (IPv4 header) - 8 (UDP header) = 65507 bytes.
 */
public class DatagramSizeTest {
    public static void main(String[] args) throws IOException {
        InetAddress addr = InetAddress.getByName("localhost");
        int port = 8080;
        // 65508 exceeds the IPv4 UDP payload limit by 1 byte and triggers an IOException
        int[] sizes = {1000, 10000, 65507, 65508};

        try (DatagramSocket socket = new DatagramSocket()) {
            for (int size : sizes) {
                try {
                    byte[] payload = new byte[size];
                    Arrays.fill(payload, (byte) 'A');
                    DatagramPacket packet = new DatagramPacket(payload, payload.length, addr, port);
                    socket.send(packet);
                    System.out.println("Sent " + size + " bytes successfully.");
                } catch (IOException e) {
                    System.err.println("Failed to send " + size + " bytes: " + e);
                }
            }
        }
    }
}