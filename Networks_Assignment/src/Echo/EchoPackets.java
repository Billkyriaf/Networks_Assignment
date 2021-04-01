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
 * from the server and handles any errors. Also monitors the response time for every packet requested .The packets received
 * are saved to files along with the request_codes used for the current session.
 * <br>
 * The class implements the {@link Structure.DataPackets} interface.
 * In many places the {@link Structure.Constants} enum is used for different String values needed
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
     * {@link #getRequest_packet_number()}
     * {@link #setRequest_packet_number(int)}
     */
    private int request_packet_number;
    /**
     * The StringBuilder that is used to construct each line byte by byte
     */
    private final StringBuilder packet = new StringBuilder();


    /**
     * Constructor
     *
     * @param connection            {@link #connection}
     * @param request_packet_number {@link #request_packet_number}
     */
    public EchoPackets(Connection connection, int request_packet_number) {
        this.connection = connection;
        this.request_packet_number = request_packet_number;

        // initialize the echo packets list
        this.echo_packets = new ArrayList<>();
    }


    /**
     * Gets the echo_packets List.
     *
     * @return List of received echo lines
     */
    public List<String> getEcho_packets() {
        return echo_packets;
    }

    /**
     * Gets the number of packets to be requested
     *
     * @return int number of packets
     */
    public int getRequest_packet_number() {
        return request_packet_number;
    }

    /**
     * Sets the number of packets to be requested
     *
     * @param request_packet_number int number of packets
     */
    public void setRequest_packet_number(int request_packet_number) {
        this.request_packet_number = request_packet_number;
    }


    /**
     * Receives large number of echo packets from the server. The function runs a loop for 5 minutes. The total number
     * of packets received is determined by the speed of the connection. A slow speed is recommended (below 10kbps).
     * <br>
     * The packets have the form:
     * <p>
     * PSTART DD-MM-YYYY HH-MM-SS PC PSTOP
     * <p>
     * Where:
     * DD-MM-YYYY date of send
     * HH-MM-SS time of send
     * PC packet counter modulo(100)
     */
    @Override
    public void getPackets() {
        int data_byte;  // The read byte from the input stream
        int response_time = -1;  // The response_time of each request
        long start_time;  // The starting time of the request
        long end_time;  // The time all the data for a certain request are completely received

        long time = System.currentTimeMillis();  // The time used to measure the duration of the complete request

        System.out.println("Receiving echo packets ...");  // DEBUG comment??

        // Request echo_packet based on the default_packet_number
        //for (int i = 0; i < this.request_packet_number; i++) {  // DEBUG comment requests specific amount of packets
        while ((System.currentTimeMillis() - time) < 300000) {  // comment if you uncomment the above line
            start_time = System.currentTimeMillis();  // Take a time measurement before the request is sent

            // Request the packet
            if (this.connection.getModem().write(this.connection.getEcho_code().getBytes())) {

                while (true) {
                    try {
                        // Read the next byte
                        data_byte = this.connection.getModem().read();

                    } catch (Exception e) {
                        // If any exception is thrown here we exit the program because something has gone wrong and there
                        // is no chance of recovering.
                        System.out.println("Exception thrown: " + e.toString());

                        System.out.println("Failed to receive echo packets. Terminating...");
                        return;
                    }

                    if (data_byte == -1) {
                        // if -1 is read there was an error and the connection timed out or dropped unexpectedly
                        System.out.println("Connection timed out. Reconnecting...");  // DEBUG comment??

                        // Try to reconnect with the server
                        if (this.connection.reconnect(3500, 10000)) {
                            // Drop the incomplete packet
                            this.packet.setLength(0);

                            System.out.println("Reconnected successfully. Continuing...");
                            break;
                        } else {
                            System.out.println("Reconnection failed. Check if the codes have expired!!");
                            return;
                        }
                    }

                    // Append to the packet string
                    this.packet.append((char) data_byte);

                    // Detect end of echo packet
                    if (isTransmissionOver()) {
                        // If the packet is complete we take a second time measurement ...
                        end_time = System.currentTimeMillis();
                        // ...and calculate the time that took the packet to arrive after the request was sent in ms.
                        long duration = (end_time - start_time);

                        response_time = Math.round(duration);

                        // System.out.println(response_time + " ms"); // DEBUG comment
                        break;
                    }
                }

                //System.out.println("Packet received: " + packet.toString());  // DEBUG comment

                if (this.packet.length() != 0) {
                    // Add packet and the corresponding response_time to the packet List
                    this.echo_packets.add(this.packet.toString() + " response_time: " + response_time + " ms");

                    // Reset packet line
                    this.packet.setLength(0);
                }

            } else {
                System.out.println("Unrecoverable exception occurred. Total echo packets received before error: " +
                        this.echo_packets.size() + ". Terminating...");
                break;
            }
        }

        // Save all received packets to a file
        saveToFile(createFileName(Constants.ECHO_DATA_DIR.getStr(), ".txt"));
    }

    /**
     * Compares the packet's (at the current state) end with the {@link Structure.Constants#PACKET_END} string.
     *
     * @return True if the pattern matched false if not or if the message was too short.
     */
    @Override
    public boolean isTransmissionOver() {
        if (this.packet.length() < Constants.PACKET_END.getStr().length())
            return false;
        else
            return this.packet.toString().endsWith(Constants.PACKET_END.getStr());
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
        File file = new File(file_name);

        try {
            file.getParentFile().mkdirs();
        } catch (SecurityException e) {
            System.out.println("Failed to create file with exception: " + e.toString());
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {

            // Write the request codes
            writer.write(
                    "####" + System.lineSeparator() +
                            this.connection.getEcho_code().substring(0, 5) + " " +
                            this.connection.getImage_code().substring(0, 5) + " " +
                            this.connection.getImage_code_error().substring(0, 5) + " " +
                            this.connection.getGps_code().substring(0, 5) + " " +
                            this.connection.getAck_code().substring(0, 5) + " " +
                            this.connection.getNack_code().substring(0, 5) +
                            System.lineSeparator() + "###" + System.lineSeparator()
            );

            // For each entry in the packets List...
            for (String str : this.echo_packets) {
                // ... write a new line
                writer.write(str + System.lineSeparator());
            }

            writer.close();

            // Clear the list so that there are no double writes in multiple files
            this.echo_packets.clear();

        } catch (IOException e) {
            System.out.println("Failed to write to file with exception: " + e.toString());
        }
    }

    /**
     * Gets the current date time and formats it in this form "yyyy-MM-dd HH-mm-ss". The final name of the file derives
     * from the directory + echo_packets yyyy-MM-dd HH-mm-ss + file extension.
     *
     * <b>Note: </b> The directory must end with / and the file extension must start with .
     *
     * @param directory      The directory the file will be saved.
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
