import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * UDP Multicast receiver that joins a multicast group on a specific IPv4 network interface,
 * prints incoming UTF-8 datagrams, and allows leaving the group via standard input.
 */
public class MulticastReceiver {
    public static void main(String[] args) throws IOException {
        // Ensure a network interface name is provided via command-line arguments
        if (args.length < 1) {
            System.out.println("Specify an active IPv4 interface name as argument:");
            printValidInterfaces();
            return;
        }

        String ifName = args[0];
        // 239.0.0.1 belongs to the 239.0.0.0/8 administratively scoped (local) multicast range
        InetAddress group = InetAddress.getByName("239.0.0.1");
        int port = 8888;

        // Look up the network interface and verify it is up and has an active IPv4 address
        NetworkInterface netIf = NetworkInterface.getByName(ifName);
        if (netIf == null || !hasIPv4(netIf)) {
            System.err.println("Interface '" + ifName + "' not found or has no active IPv4.");
            printValidInterfaces();
            return;
        }

        // MulticastSocket automatically enables SO_REUSEADDR, allowing multiple instances on the same port
        try (MulticastSocket socket = new MulticastSocket(port)) {
            InetSocketAddress groupAddr = new InetSocketAddress(group, port);

            // Subscribe to the multicast group on the chosen network interface (Java 14+ non-deprecated API)
            socket.joinGroup(groupAddr, netIf);
            System.out.println("Joined 239.0.0.1:8888 on " + ifName + " (" + netIf.getDisplayName() + ")");
            System.out.println("Press Enter in THIS console to leave the group...");

            // Background thread that monitors this instance's console input so it can leave the group independently
            Thread consoleThread = new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                    in.readLine(); // Blocks until the user presses "Enter" in this receiver's console
                    socket.leaveGroup(groupAddr, netIf);
                    System.out.println("Left multicast group.");
                } catch (IOException e) {
                    System.err.println("Error leaving group: " + e.getMessage());
                }
            });
            // Mark as daemon so this thread does not prevent the JVM from exiting when the main loop ends
            consoleThread.setDaemon(true);
            consoleThread.start();

            // Continuously listen for incoming multicast datagrams
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Blocks until a datagram arrives

                // Convert only the received bytes (0 to packet.getLength()) into a UTF-8 String
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                System.out.println("[" + packet.getAddress().getHostAddress() + "] " + msg);
            }
        }
    }

    /**
     * Checks whether the given network interface is active and configured with at least one IPv4 address.
     */
    private static boolean hasIPv4(NetworkInterface ni) throws SocketException {
        return ni.isUp() && Collections.list(ni.getInetAddresses()).stream()
                .anyMatch(addr -> addr instanceof Inet4Address);
    }

    /**
     * Prints all active network interfaces on the system that have an IPv4 address.
     */
    private static void printValidInterfaces() throws SocketException {
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (hasIPv4(ni)) {
                String ipv4 = Collections.list(ni.getInetAddresses()).stream()
                        .filter(addr -> addr instanceof Inet4Address)
                        .map(InetAddress::getHostAddress)
                        .findFirst()
                        .orElse("");
                System.out.println(" - " + ni.getName() + " [" + ipv4 + "] (" + ni.getDisplayName() + ")");
            }
        }
    }
}