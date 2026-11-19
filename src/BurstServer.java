import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

/**
 * Measures UDP packet loss during a 10,000-datagram burst (Section 4, Step 13).
 * Optional sleep parameter simulates a slow receiver whose kernel buffer overflows.
 */
public class BurstServer {
    public static void main(String[] args) throws Exception {
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : 8080;
        int sleepMs = (args.length >= 2) ? Integer.parseInt(args[1]) : 0;
        int expectedTotal = 10_000;
        int received = 0;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("BurstServer ready on port " + port + " (sleep=" + sleepMs + "ms)");
            byte[] buf = new byte[256];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                    if (received == 0) {
                        // Start a 2-second inactivity timer once the first burst packet arrives
                        socket.setSoTimeout(2000);
                    }
                    received++;

                    // Artificial processing delay to induce kernel socket buffer drops (Step 13c)
                    if (sleepMs > 0) {
                        Thread.sleep(sleepMs);
                    }
                } catch (SocketTimeoutException e) {
                    // Burst ended: compute and display packet loss percentage
                    double loss = ((expectedTotal - received) * 100.0) / expectedTotal;
                    System.out.printf("Received %d / %d (Loss rate: %.2f%%)%n", received, expectedTotal, loss);
                    received = 0;
                    socket.setSoTimeout(0); // Reset to blocking wait for the next burst
                }
            }
        }
    }
}