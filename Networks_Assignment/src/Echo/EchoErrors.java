package Echo;

import Structure.Connection;
import Structure.Constants;
import Structure.DataPackets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * <h1>EchoErrors Class.</h1>
 * EchoErrors is the class that handles all the actions regarding the echo packets that contain pseudo errors from the
 * server. The class requests echo packets from the server and handles any errors with the use of ACK and NACK codes.
 * Also monitors the response time for every packet requested .The packets received are saved to files along with the
 * request_codes used for the current session.
 * <br>
 * The class implements the {@link Structure.DataPackets} interface.
 * In many places the {@link Structure.Constants} enum is used for different String values needed
 *
 * @author Vasilis Kyriafinis
 * @version 1.0
 * @since 1.0
 */
public class EchoErrors implements DataPackets {

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
    public EchoErrors(Connection connection, int request_packet_number) {
        super();
        this.connection = connection;
        this.echo_packets = new ArrayList<>();
        this.request_packet_number = request_packet_number;
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
     * Receives large number of echo packets with errors from the server. Every time a packet's checksum is not valid
     * the nac code is send and the packet is received again. Finally the packets are saved to a file for future
     * reference.
     *
     * The packets have the form:
     * <p>
     * PSTART DD-MM-YYYY HH-MM-SS PC <XXXXXXXXXXXXXXXX> FCS PSTOP
     * <p>
     * Where:
     * DD-MM-YYYY date of send
     * HH-MM-SS time of send
     * PC packet counter modulo(100)
     * <XXXXXXXXXXXXXXXX> the data of the packet
     * FCS the CheckSum8 Xor for the data without the <>
     */
    @Override
    public void getPackets() {
        int response_time = -1;  // The response_time of the request

        System.out.println("Receiving echo packets with errors ...");  // DEBUG comment??

        // Request echo_packet based on the default_packet_number
        for (int i = 0; i < this.request_packet_number; i++) {
            boolean check_sum_ok = false;
            String requestCode = this.connection.getAck_code();

            // Loop for every packet until it is received with no errors
            while (!check_sum_ok) {
                // Reset the string builder every time before reading the next packet
                this.packet.setLength(0);

                // Request the packet
                response_time = readPacket(requestCode);

                if (response_time != -1) {
                    //System.out.println(this.packet);  // DEBUG comment??

                    check_sum_ok = isCheckSumOk(this.packet.toString());
                } else {
                    System.out.println("Connection error!! Terminating...");
                    return;
                }

                requestCode = this.connection.getNack_code();
            }

            if (this.packet.length() != 0) {
                // Add packet and the corresponding response_time to the packet List
                this.echo_packets.add(this.packet.toString() + " response_time: " + response_time + " ms");

                // Reset packet line
                this.packet.setLength(0);
            }
        }

        // Save all received packets to a file
        saveToFile(createFileName(Constants.ERR_ECHO_DATA_DIR.getStr(), ".txt"));
    }


    /**
     * Compares the packet's (at the current state) end with the {@link Structure.Constants#PACKET_END} string.
     *
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
        File file = new File(file_name);

        try {
            file.getParentFile().mkdirs();
        }
        catch (SecurityException securityException) {
            System.out.println("Failed to create file: " + securityException.toString());
            return;
        }

        try {
            FileWriter writer = new FileWriter(file);

            // Write the request codes
            writer.write("####\n" + this.connection.getEcho_code() + this.connection.getImage_code() +
                    this.connection.getImage_code_error() + this.connection.getGps_code() +
                    this.connection.getAck_code() + this.connection.getNack_code() + "###" +
                    System.lineSeparator());

            // For each entry in the packets List...
            for (String str : this.echo_packets) {
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

        String name = Constants.ERR_ECHO_FILE_NAME.getStr();

        String pattern = "yyyy-MM-dd HH-mm-ss";

        // formatter for the string representation of date according to the chosen pattern
        DateFormat df = new SimpleDateFormat(pattern);

        // Get the today date using Calendar object.
        Date today = Calendar.getInstance().getTime();

        return directory + name + df.format(today) + file_extension;
    }

    private int readPacket(String request_code) {
        int k;  // Each input byte

        long start_time = System.nanoTime();  // Take a time measurement before the packet is sent
        int response_time = -1;  // The response_time of the request

        // Request the packet with the ack code
        if (this.connection.getModem().write(request_code.getBytes())) {

            while (true) {
                try {
                    // Read the next byte
                    k = this.connection.getModem().read();

                } catch (Exception x) {
                    System.out.println("Exception thrown: " + x.toString());

                    // Reset packet line
                    this.packet.setLength(0);
                    break;
                }

                // if -1 is read there was an error and the connection timed out
                if (k == -1) {
                    System.out.println("Connection timed out.");  // DEBUG comment??
                    break;
                }

                // Append to the packet string
                this.packet.append((char) k);

                // Detect end of echo packet
                if (isTransmissionOver()) {
                    // If the line is complete we take a second time measurement ...
                    long end_time = System.nanoTime();
                    // ...and calculate the time that took the packet to arrive after the request was sent in ms.
                    double duration = (end_time - start_time) / 1000000.0;

                    response_time = (int) Math.round(duration);

                    break;
                }
            }
        }

        return response_time;
    }

    /**
     * Checks the CheckSum8 Xor for the data included in the packet.
     * @param data_packet the hole received packet
     * @return true is the checksum matches false if not
     */
    private boolean isCheckSumOk(String data_packet) {
        // Isolate the data and the check sum
        String message = data_packet.split("<")[1].split(" PSTOP")[0];

        // Get the data WARNING!! substring() upper limit is exclusive
        String data = message.substring(0, 16);

        // Parse the checksum
        int check_sum = Integer.parseInt(message.substring(18));

        int XOR = data.charAt(0);
        for (int i = 1; i < data.length(); i++) {
            XOR = XOR ^ data.charAt(i);
        }

        //System.out.println(data);  // Debug comment
        //System.out.println(XOR);  // Debug comment

        return XOR == check_sum;
    }
}