package UserApp;

import java.util.ArrayList;
import java.util.List;

public class EchoPackets {

    private final Connection connection;
    private final List<String> echo_packets;

    /**
     * Constructor
     * @param connection the connection object
     */
    public EchoPackets(Connection connection){
        this.connection = connection;

        // initialize the echo packets list
        this.echo_packets = new ArrayList<>();
    }


    /**
     * Receives large number of echo packets from the server. The packets are:
     *
     * PSTART DD-MM-YYYY HH-MM-SS PC PSTOP
     *
     * Where:
     *     DD-MM-YYYY date of send
     *     HH-MM-SS time of send
     *     PC packet counter modulo(100)
     *
     * @param packet_counter The number of packets to be requested
     *
     */
    public void getEchoPackets(int packet_counter) {
        int k;  // The input buffer byte
        StringBuilder packet = new StringBuilder();  // Complete packet

        for (int i = 0; i <= packet_counter; i++) {
            // Request the echo_packet
            if (this.connection.getModem().write(this.connection.getEcho_code().getBytes())) {
                while (true) {
                    try {
                        // Read the bytes
                        k = this.connection.getModem().read();

                        // if -1 is read there was an error and the connection timed out
                        if (k == -1) {
                            System.out.println("Connection timed out.");
                            break;
                        }

                        // Append to the packet string
                        packet.append((char) k);

                        // Detect end of packet
                        if (this.connection.isMessageOver(packet.toString(), Commands.PACKET_END.getStr())) break;

                    } catch (Exception x) {
                        System.out.println("Exception thrown: " + x.toString());
                        break;
                    }
                }

                System.out.println("Packet received: " + packet.toString());

                // Add packet to the list
                this.echo_packets.add(packet.toString());

                // Reset packet
                packet.setLength(0);

            } else {
                System.out.println("Failed to send echo code");
                break;
            }
        }
    }
}
