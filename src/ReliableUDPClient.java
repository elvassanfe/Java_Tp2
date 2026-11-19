import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Reliable UDP client using Stop-and-Wait ARQ (Section 4, Steps 11 & 12).
 * Prefixes each message with a sequence number and retransmits on timeout.
 */
public class ReliableUDPClient {
    private static final int TIMEOUT_MS = 500;
    private static final int MAX_ATTEMPTS = 5;

    public static void main(String[] args) throws IOException {
        String host = (args.length >= 1) ? args[0] : "localhost";
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 8080;

        try (DatagramSocket socket = new DatagramSocket();
             BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            // Configure receive timeout so socket.receive() throws SocketTimeoutException
            socket.setSoTimeout(TIMEOUT_MS);
            InetAddress serverAddr = InetAddress.getByName(host);
            int seq = 0;
            String line;

            while ((line = in.readLine()) != null) {
                String framed = seq + ":" + line;
                byte[] data = framed.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddr, port);

                boolean acked = false;
                for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                    socket.send(packet);
                    try {
                        byte[] ackBuf = new byte[64];
                        DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length);
                        socket.receive(ackPacket);

                        String ackMsg = new String(
                                ackPacket.getData(), 0, ackPacket.getLength(), StandardCharsets.UTF_8);
                        if (ackMsg.equals("ACK:" + seq)) {
                            System.out.println("Delivered seq=" + seq + " (attempt " + attempt + ")");
                            acked = true;
                            break;
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("Timeout for seq=" + seq + " (attempt " + attempt + "/" + MAX_ATTEMPTS + ")");
                    }
                }

                if (!acked) {
                    System.err.println("ERROR: Failed to deliver seq=" + seq + " after " + MAX_ATTEMPTS + " attempts.");
                }
                seq++;
            }
        }
    }
}