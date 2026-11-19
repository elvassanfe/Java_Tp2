import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

/**
 * Reliable UDP server implementing sequence checking, ACKs, duplicate detection,
 * and simulated packet/ACK loss (Section 4, Steps 11, 12 & 14).
 */
public class ReliableUDPServer {
    public static void main(String[] args) throws IOException {
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : 8080;
        // Optional drop probability (e.g., 0.2 = 20% loss) for Step 14 testing
        double dropProbability = (args.length >= 2) ? Double.parseDouble(args[1]) : 0.0;

        int expectedSeq = 0;
        int lastReceivedSeq = -1;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("ReliableUDPServer listening on port " + port
                    + " (dropRate=" + dropProbability + ")");
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Simulate lost incoming data packet (Step 14)
                if (Math.random() < dropProbability) {
                    System.out.println("[SIMULATED DROP] Dropped incoming packet");
                    continue;
                }

                // Parse frame format "<seq>:<message>" (Step 11)
                String raw = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                int sep = raw.indexOf(':');
                if (sep == -1) continue;

                int seq = Integer.parseInt(raw.substring(0, sep));
                String payload = raw.substring(sep + 1);

                // Detect duplicates caused by a lost ACK (Step 14)
                if (seq <= lastReceivedSeq) {
                    System.out.println("[DUPLICATE] seq=" + seq + " ignored, re-sending ACK");
                } else {
                    if (seq > expectedSeq) {
                        System.out.println("[WARNING] Missing packets: expected " + expectedSeq + ", got " + seq);
                    }
                    System.out.println("[seq=" + seq + "] " + payload);
                    lastReceivedSeq = seq;
                    expectedSeq = seq + 1;
                }

                // Simulate lost outgoing ACK to verify duplicate handling on client retry (Step 14)
                if (Math.random() < dropProbability) {
                    System.out.println("[SIMULATED DROP] Dropped ACK:" + seq);
                    continue;
                }

                // Send positive acknowledgment back to sender's IP and port (Step 12)
                byte[] ackBytes = ("ACK:" + seq).getBytes(StandardCharsets.UTF_8);
                DatagramPacket ackPacket = new DatagramPacket(
                        ackBytes, ackBytes.length, packet.getAddress(), packet.getPort());
                socket.send(ackPacket);
            }
        }
    }
}