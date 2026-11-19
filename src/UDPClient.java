import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * UDP client (Sections 1, 3 & 5).
 * Reads lines from standard input and sends each line in a UTF-8 datagram.
 */
public class UDPClient {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java UDPClient <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        // Ephemeral source port is automatically assigned by the OS
        try (DatagramSocket socket = new DatagramSocket()) {
            // Allow sending to local broadcast addresses such as 255.255.255.255 (Step 18)
            socket.setBroadcast(true);
            InetAddress serverAddr = InetAddress.getByName(host);

            // System.console() is null inside IDEs; fall back to BufferedReader on System.in
            Console console = System.console();
            if (console != null) {
                String line;
                while ((line = console.readLine()) != null) {
                    sendLine(socket, serverAddr, port, line);
                }
            } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(System.in, StandardCharsets.UTF_8));
                String line;
                while ((line = in.readLine()) != null) {
                    sendLine(socket, serverAddr, port, line);
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    /** Encodes a string in UTF-8 and transmits it as a single UDP datagram. */
    private static void sendLine(DatagramSocket socket, InetAddress addr, int port, String line)
            throws IOException {
        byte[] data = line.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
        socket.send(packet);
    }
}