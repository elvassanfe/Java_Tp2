[project_readme.md](https://github.com/user-attachments/files/35111990/project_readme.md)
# TP 2: UDP Client-Server with Java Sockets

**Course:** Java for Networks — 3RTS (ENSEA)  
**Instructor:** Linda Senigagliesi  
**Academic Year:** 2026–2027  

## Overview

This repository contains the Java implementation and experimental exercises for **Lab 2 (TP 2)** on **UDP Datagram Sockets**. Through progressive implementations, the project explores the fundamental differences between UDP and TCP:

1. **Connectionless Unicast Communication:** Building a basic UTF-8 UDP server and client without handshakes or persistent connection state.
2. **Datagram Size Limits & Fragmentation:** Testing receiver buffer truncation and maximum IPv4 UDP payload boundaries ($65\,535 - 20 - 8 = 65\,507\text{ bytes}$).
3. **Reliability over UDP (Stop-and-Wait ARQ):** Adding sequence numbers, positive acknowledgments (`ACK:n`), timeout-based retransmissions, and duplicate detection on top of unreliable datagrams.
4. **Packet Loss Measurement:** Sending high-speed bursts of $10\,000$ datagrams to observe OS kernel socket receive buffer (`SO_RCVBUF`) overflow.
5. **One-to-Many Communication:** Implementing IPv4 UDP Multicast (`239.0.0.1:8888`) and contrasting it with local subnet Broadcast (`255.255.255.255`).

---

## Prerequisites

* **Java Development Kit (JDK) 14 or higher** (JDK 17 / 21 / 24 recommended).  
  *Note: JDK 14+ is required for the non-deprecated `MulticastSocket.joinGroup(SocketAddress, NetworkInterface)` method used in `MulticastReceiver`.*
* **IntelliJ IDEA** (or any terminal with `javac` and `java`).
* Optional: `nc` (Linux/macOS), `ncat` (Windows Nmap), and **Wireshark** for packet inspection.

---

## Project Structure

```text
Java_Tp2/
├── src/
│   ├── UDPServer.java           # Basic connectionless UDP server with configurable port & buffer size
│   ├── UDPClient.java           # Interactive UTF-8 UDP client (supports unicast, multicast & broadcast)
│   ├── DatagramSizeTest.java    # Tests IPv4 payload limits (1000 B to 65508 B)
│   ├── ReliableUDPServer.java   # Server with sequence validation, ACKs, duplicate filtering & simulated drops
│   ├── ReliableUDPClient.java   # Stop-and-Wait client with sequence numbering and timeout retransmissions
│   ├── BurstServer.java         # Counts burst datagrams and calculates packet loss rate
│   ├── BurstClient.java         # Sends a rapid burst of 10,000 numbered datagrams
│   └── MulticastReceiver.java   # Joins multicast group 239.0.0.1:8888 on a chosen IPv4 interface
└── README.md
```

---

## Compilation & Execution

### From the Command Line

Compile all classes into an `out` directory:

```bash
javac -d out src/*.java
```

Run any class using `java -cp out <ClassName> [arguments]`.

---

## Lab Sections & Usage

### 1. Basic UDP Client-Server (Steps 1–4)

* **Start the server** (defaults to port `8080` and buffer size `1024` bytes):
  ```bash
  java -cp out UDPServer 8080
  ```
* **Start one or more clients** (type messages and press `Enter`; send EOF with `Ctrl+D` on Linux/macOS or `Ctrl+Z` + `Enter` on Windows):
  ```bash
  java -cp out UDPClient localhost 8080
  ```
* **Quick test with Netcat:**
  ```bash
  nc -u localhost 8080      # Linux / macOS
  ncat -u localhost 8080    # Windows (with Nmap installed)
  ```

### 2. Datagram Size & Fragmentation Analysis (Steps 5–8)

* **Test buffer truncation** with a 64-byte receive buffer:
  ```bash
  java -cp out UDPServer 8080 64
  ```
  *Observation:* Messages longer than 64 bytes are silently truncated by the OS; excess bytes are discarded rather than delivered in a second datagram, and the sender is not notified.

* **Test maximum IPv4 UDP payload** with a $65\,507$-byte buffer:
  ```bash
  java -cp out UDPServer 8080 65507
  java -cp out DatagramSizeTest
  ```
  *Observation:* Datagrams of $1\,000$, $10\,000$, and $65\,507$ bytes succeed ($10\,000$ bytes splits into $7$ IPv4 fragments over a $1500$-byte Ethernet MTU). Sending $65\,508$ bytes throws `IOException: Message too long`.

### 3. Reliability & Packet Loss Measurement (Steps 11–15)

* **Stop-and-Wait ARQ with simulated 20% packet/ACK drop rate:**
  ```bash
  java -cp out ReliableUDPServer 8080 0.2
  java -cp out ReliableUDPClient localhost 8080
  ```
  *Behavior:* The client prefixes each line with a sequence number (`0:hello`) and retransmits after a $500\text{ ms}$ `SocketTimeoutException` (up to 5 attempts). The server ignores duplicate sequence numbers (`seq <= lastReceivedSeq`) caused by lost acknowledgments while re-sending `ACK:n`.

* **Burst packet loss measurement ($N = 10\,000$ datagrams):**
  ```bash
  # Case a) Fast receiver on localhost (0 ms delay)
  java -cp out BurstServer 8080 0
  java -cp out BurstClient localhost 8080

  # Case c) Slow receiver on localhost (2 ms sleep per packet -> kernel SO_RCVBUF overflow)
  java -cp out BurstServer 8080 2
  java -cp out BurstClient localhost 8080
  ```

### 4. UDP Multicast & Broadcast (Steps 16–19)

* **List active IPv4 network interfaces** (run without arguments):
  ```bash
  java -cp out MulticastReceiver
  ```
* **Start multiple multicast receivers** (replace `<interface>` with your active IPv4 NIC, e.g., `eth0`, `en0`, `wlan1`, or `lo`):
  ```bash
  java -cp out MulticastReceiver <interface>
  ```
  *Press `Enter` inside a receiver's console at any time to invoke `leaveGroup()` on that specific instance.*

* **Send to the multicast group (`239.0.0.1:8888`):**
  ```bash
  java -cp out UDPClient 239.0.0.1 8888
  ```

* **Send a local subnet broadcast (`255.255.255.255:8888`):**
  ```bash
  java -cp out UDPClient 255.255.255.255 8888
  ```
