package Echo;

import Structure.DataPackets;
import Structure.Constants;
import Structure.Connection;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * <h1>EchoPackets Class.</h1>
 * EchoPackets is the class that handles all the actions regarding the echo packets. The class requests echo packets
 * from the server and handles any errors. Also monitors the latency for every packet requested .The packets received
 * are saved to files along with the request_codes used for the current session.
 * <br>
 * The class implements the {@link Structure.DataPackets} interface.
 * In many places the {@link Structure.Constants} enum is used for different String values needed
 *
 *
 * @author Vasilis Kyriafinis
 * @version 1.0
 * @since 1.0
 */
public class EchoPackets implements DataPackets {

    /**
     * The {@link Structure.Connection} instance of the server connection
     */
    private final Connection connection;
    /**
     * The List that holds the complete lines of the packets received
     * {@link #getEcho_packets()}
     */
    private final List<String> echo_packets;  // List to save all the packets
    /**
     * The number of packets to be requested
     * {@link #getDefault_packet_number()}
     * {@link #setDefault_packet_number(int)}
     */
    private int default_packet_number;
    /**
     * The StringBuilder that is used to construct each line byte by byte
     */
    private final StringBuilder packet = new StringBuilder();


    /**
     * Constructor
     * @param connection {@link #connection}
     * @param default_packet_number {@link #default_packet_number}
     */
    public EchoPackets(Connection connection, int default_packet_number){
        this.connection = connection;
        this.default_packet_number = default_packet_number;

        // initialize the echo packets list
        this.echo_packets = new ArrayList<>();
    }


    /**
     * Gets the echo_packets List.
     * @return List of received echo lines
     */
    public List<String> getEcho_packets() {
        return echo_packets;
    }


    /**
     * Gets the number of packets to be requested
     * @return int number of packets
     */
    public int getDefault_packet_number() {
        return default_packet_number;
    }


    /**
     * Sets the number of packets to be requested
     * @param default_packet_number int number of packets
     */
    public void setDefault_packet_number(int default_packet_number) {
        this.default_packet_number = default_packet_number;
    }


    /**
     * Receives large number of echo packets from the server. The packets have the form:
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
        int latency = -1;  // The latency of each request

        System.out.println("Receiving echo packets ...");  // DEBUG comment??

        // Request echo_packet based on the default_packet_number
        for (int i = 0; i < this.default_packet_number; i++) {

            long startTime = System.nanoTime();  // Take a time measurement before the packet is sent

            // Request the packet
            if (this.connection.getModem().write(this.connection.getEcho_code().getBytes())) {

                while (true) {
                    try {
                        // Read the next byte
                        k = this.connection.getModem().read();


                        // if -1 is read there was an error and the connection timed out
                        if (k == -1) {
                            System.out.println("Connection timed out.");  // DEBUG comment??
                            break;
                        }

                        // Append to the packet string
                        this.packet.append((char) k);

                        // Detect end of echo packet
                        if (isTransmissionOver()) {
                            // If the line is complete...
                            // ... we take a second time measurement ...
                            long endTime = System.nanoTime();
                            // ...and calculate the time that took the packet to arrive after the request was sent in ms.
                            double duration = (endTime - startTime)/1000000.0;

                            latency = (int) Math.round(duration);

                            // System.out.println(latency + " ms"); // DEBUG comment
                            break;
                        }

                    } catch (Exception x) {
                        System.out.println("Exception thrown: " + x.toString());
                        break;
                    }
                }

                //System.out.println("Packet received: " + packet.toString());  // DEBUG comment

                // Add packet and the corresponding latency to the packet List
                this.echo_packets.add(packet.toString() + " latency: " + latency + " ms");

                // Reset packet line
                packet.setLength(0);

            } else {
                System.out.println("Failed to send echo code");
                break;
            }
        }

        // Save all received packets to a file
        saveToFile(createFileName(Constants.ECHO_DATA_DIR.getStr(), ".txt"));
    }

    /**
     * Compares the packet's (at the current state) end with the {@link Structure.Constants#PACKET_END} string.
     * @return True if the pattern matched false if not or if the message was too short.
     */
    @Override
    public boolean isTransmissionOver() {
        if (this.packet.length() < Constants.PACKET_END.getStr().length()) {
            return false;
        } else return this.packet.toString().endsWith(Constants.PACKET_END.getStr());
    }

    /**
     * Save all the echo packets and latencies form the packet List to a file. The file starts with ## request_codes ##
     * for later identification. Every echo packet and the corresponding latency are saved to a new line.
     * <br>
     * Use the {@link #createFileName(String, String)} method to obtain the correct file name.
     *
     * @param file_name The name of the file.
     */
    @Override
    public void saveToFile(String file_name) {
        try {
            FileWriter writer = new FileWriter(file_name);

            // Write the request codes
            writer.write("## " + this.connection.getEcho_code() + " " + this.connection.getImage_code() + " " +
                    this.connection.getImage_code_error() + " " + this.connection.getGps_code() + " " +
                    this.connection.getAck_result_code() + " " + this.connection.getNack_result_code() + " ##" +
                    System.lineSeparator());

            // For each entry in the packets List...
            for(String str: this.echo_packets) {
                // ... write a new line
                writer.write(str + System.lineSeparator());
            }


            writer.close();

            // Clear the list so that there are no double writes in multiple files
            this.echo_packets.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the current date time and formats it in this form "yyyy-MM-dd HH-mm-ss". The final name of the file derives
     * from the directory + echo_packets yyyy-MM-dd HH-mm-ss + file extension.
     *
     * <b>Note: </b> The directory must end with / and the file extension must start with .
     *
     * @param directory The directory the file will be saved.
     * @param file_extension The type of the file e.g.  .txt
     * @return directory + name + date + file_extension
     */
    @Override
    public String createFileName(String directory, String file_extension) {

        String name = Constants.ECHO_FILE_NAME.getStr();

        String pattern = "yyyy-MM-dd HH-mm-ss";

        // formatter for the string representation of date according to the chosen pattern
        DateFormat df = new SimpleDateFormat(pattern);

        // Get the today date using Calendar object.
        Date today = Calendar.getInstance().getTime();

        return directory + name + df.format(today) + file_extension;
    }
}
