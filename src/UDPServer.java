import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Basic connectionless UDP server (Sections 1 & 2).
 * Listens on a specified UDP port and prints incoming UTF-8 messages.
 */
public class UDPServer {
    private final int port;
    private final int bufferSize;
    private boolean running;

    /** Constructor with custom port and default 1024-byte buffer (Step 1). */
    public UDPServer(int port) {
        this(port, 1024);
    }

    /** Constructor with custom port and custom receive buffer size (Step 5). */
    public UDPServer(int port, int bufferSize) {
        this.port = port;
        this.bufferSize = bufferSize;
        this.running = false;
    }

    /** Default constructor using port 8080 and a 1024-byte buffer (Step 1). */
    public UDPServer() {
        this(8080, 1024);
    }

    /** Starts the server socket and receives datagrams in an infinite loop. */
    public void launch() {
        // Create an unbound socket first to enable SO_REUSEADDR before binding
        try (DatagramSocket socket = new DatagramSocket(null)) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));

            running = true;
            System.out.println(this);
            byte[] buffer = new byte[bufferSize];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Blocks until a datagram arrives

                // Decode only the actual number of bytes received (packet.getLength())
                String msg = new String(
                        packet.getData(),
                        0,
                        packet.getLength(),
                        StandardCharsets.UTF_8
                );

                // Prefix message with sender's IP address and ephemeral port
                System.out.println("[" + packet.getAddress().getHostAddress()
                        + ":" + packet.getPort() + "] " + msg);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            running = false;
        }
    }

    @Override
    public String toString() {
        return "UDPServer{port=" + port + ", bufferSize=" + bufferSize + ", running=" + running + "}";
    }

    public static void main(String[] args) {
        // args[0]: listening port (default 8080), args[1]: receive buffer size (default 1024)
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : 8080;
        int bufSize = (args.length >= 2) ? Integer.parseInt(args[1]) : 1024;
        new UDPServer(port, bufSize).launch();
    }
}