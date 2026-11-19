import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Sends a rapid burst of 10,000 numbered UDP datagrams without ACKs (Section 4, Step 13).
 */
public class BurstClient {
    public static void main(String[] args) throws IOException {
        String host = (args.length >= 1) ? args[0] : "localhost";
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 8080;
        int n = 10_000;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress addr = InetAddress.getByName(host);
            for (int i = 0; i < n; i++) {
                byte[] data = (i + ":burst").getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(data, data.length, addr, port));
            }
            System.out.println("Sent " + n + " datagrams.");
        }
    }
}