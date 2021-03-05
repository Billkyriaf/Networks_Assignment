package Echo;

import Stracture.DataPackets;
import Stracture.Constants;
import Stracture.Connection;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EchoPackets implements DataPackets {

    private final Connection connection;
    private final List<String> echo_packets;
    private int default_packet_number;
    private final StringBuilder packet = new StringBuilder();  // Complete packet

    /**
     * Constructor
     * @param connection the connection object
     * @param default_packet_number  the number of the packets to be received
     */
    public EchoPackets(Connection connection, int default_packet_number){
        this.connection = connection;
        this.default_packet_number = default_packet_number;

        // initialize the echo packets list
        this.echo_packets = new ArrayList<>();
    }



    // ===============================  Getters - Setters  ===============================

    public List<String> getEcho_packets() {
        return echo_packets;
    }

    public int getDefault_packet_number() {
        return default_packet_number;
    }

    public void setDefault_packet_number(int default_packet_number) {
        this.default_packet_number = default_packet_number;
    }



    // ===============================  Interface methods  ===============================

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
     */
    @Override
    public void getPackets() {
        int k;  // The input buffer byte
        int latency = -1;

        System.out.println("Receiving echo packets ...");

        for (int i = 0; i < this.default_packet_number; i++) {
            // Request the echo_packet

            long startTime = System.nanoTime();  // Take a measurement before the packet is sent for latency monitoring

            if (this.connection.getModem().write(this.connection.getEcho_code().getBytes())) {

                while (true) {
                    try {
                        // Read the next byte
                        k = this.connection.getModem().read();


                        // if -1 is read there was an error and the connection timed out
                        if (k == -1) {
                            System.out.println("Connection timed out.");
                            break;
                        }

                        // Append to the packet string
                        this.packet.append((char) k);


                        // Detect end of packet
                        if (isTransmissionOver()) {

                            // Latency measurement
                            long endTime = System.nanoTime();
                            double duration = ((endTime - startTime)/1000000.0);  // divide by 1000000 to get milliseconds.
                            latency = (int) Math.round(duration);
                            // System.out.println(latency + " ms");

                            break;
                        }



                    } catch (Exception x) {
                        System.out.println("Exception thrown: " + x.toString());
                        break;
                    }
                }

                //System.out.println("Packet received: " + packet.toString());  // DEBUG comment

                // Add packet to the list
                this.echo_packets.add(packet.toString() + " latency: " + latency + " ms");

                // Reset packet
                packet.setLength(0);

            } else {
                System.out.println("Failed to send echo code");
                break;
            }
        }

        // Save packets to a file
        saveToFile(createFileName(Constants.ECHO_DATA_DIR.getStr(), ".txt"));
    }

    /**
     * Compares the packet's (at the current state) end with the end packet string
     * @return True if the pattern matched false if not or if the message was too short
     */
    @Override
    public boolean isTransmissionOver() {
        String packet_end = Constants.PACKET_END.getStr();
        if (this.packet.length() < packet_end.length()) {
            return false;
        } else return this.packet.toString().endsWith(packet_end);
    }

    /**
     * Save all the echo packets form the list to a file
     * @param file_name the name of the file
     */
    @Override
    public void saveToFile(String file_name) {
        try {
            FileWriter writer = new FileWriter(file_name);

            writer.write("## " + this.connection.getEcho_code() + " " + this.connection.getImage_code() + " " +
                    this.connection.getImage_code_error() + " " + this.connection.getGps_code() + " " +
                    this.connection.getAck_result_code() + " " + this.connection.getNack_result_code() + " ##" +
                    System.lineSeparator());

            for(String str: this.echo_packets) {
                writer.write(str + System.lineSeparator());
            }
            writer.close();

            this.echo_packets.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the current date time and formats it "yyyy-MM-dd HH-mm-ss"
     * @return name + date + .txt
     */
    @Override
    public String createFileName(String directory, String file_extension) {
        // Create the name of the image file depending on the errors
        String name = "echo_packets ";

        String pattern = "yyyy-MM-dd HH-mm-ss";

        // Create an instance of SimpleDateFormat used for formatting
        // the string representation of date according to the chosen pattern
        DateFormat df = new SimpleDateFormat(pattern);

        // Get the today date using Calendar object.
        Date today = Calendar.getInstance().getTime();

        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.

        return directory + name + df.format(today) + file_extension;
    }
}
