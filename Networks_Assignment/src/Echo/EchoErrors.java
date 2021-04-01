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
 * server. The class requests echo packets from the server. The main purpose of the class is to implement an ARQ mechanism
 * for error detection and correction. This mechanism is making use of the ACK and NACK codes to request a packet again
 * if it has errors in the data part.
 * It also monitors the response time for every packet requested .The packets received are saved to files along with the
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
     * The getPackets function is responsible for evaluating packets received by {@link #readPacket(String ACK_CODE)}.
     * It also handles retransmission due to checksum pseudo errors inserted by the server. Every time a packet's
     * checksum, calculated by {@link #isCheckSumOk(String packet)}, is not valid the {@link #readPacket(String NAC_CODE)}
     * is called and the packet is received again. Finally the packets are saved to the {@link #echo_packets} List and
     * to a file by {@link #saveToFile(String file_name)} for future reference.
     * <br>
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
        int packet_count = 1;

        System.out.println("Receiving echo packets with errors ...");

        long time = System.currentTimeMillis();  // The time used to measure the duration of the complete request

        //for (int i = 0; i < this.request_packet_number; i++) {  // DEBUG comment requests specific amount of packets
        while ((System.currentTimeMillis() - time) < 300000) {  // comment if you uncomment the above line

            System.out.println("Receiving packet: " + packet_count++);

            String requestCode = this.connection.getAck_code();  // The ACK code for the packet request
            int retransmissions = 0;  // The number each individual packet is retransmitted. Zero means the packet was received correctly the first time.


            // Request the packet
            long start_time = readPacket(requestCode);  // The starting time of the request

            // Loop for every packet until it is received with no errors
            while (true) {
                if (start_time != -1) {
                    //System.out.println(this.packet);  // DEBUG comment

                    if (isCheckSumOk(this.packet.toString())) {
                        // If the check sum is correct we take a second time measurement ...
                        long end_time = System.currentTimeMillis();
                        // ...and calculate the time that took the packet to arrive after the request was sent in ms.
                        long duration = (end_time - start_time);

                        response_time = Math.round(duration);
                        break;
                    } else {
                        // If we enter the else block the packet was not received correctly so we must...
                        requestCode = this.connection.getNack_code();  // ...change the request code to the NACK code...
                        this.packet.setLength(0);  // ...drop the previously received packet...
                        readPacket(requestCode);  // ...and request the packet one more
                        retransmissions++;
                    }
                } else {
                    /* If the start_time is returned -1 from the readPacket function some error has occurred.
                     * Either the connection timed out or dropped, or an exception was thrown.
                     * In the last case the exception will be printed to the console.
                     */
                    System.out.println("Connection error!! Terminating...");
                    return;
                }
            }

            if (this.packet.length() != 0) {
                // Add packet the response_time and the count of retransmissions to the packet List
                this.echo_packets.add(this.packet.toString() + " response_time: " + response_time + " ms " +
                        "retransmissions: " + retransmissions);

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
        if (this.packet.length() < Constants.PACKET_END.getStr().length())
            return false;
        else
            return this.packet.toString().endsWith(Constants.PACKET_END.getStr());
    }

    /**
     * Save all the echo packets and latencies form the packet List to a file. The file starts with
     * "####\n" request_codes "###\n" for later identification of the session. Every echo packet along with the
     * response time and the number of retransmissions are saved to a new line.
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
     * <b>Note: </b>The directory must end with "/" and the file extension must start with "."
     *
     * @param directory      The directory the file will be saved.
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

    /**
     * The readPacket function is responsible for requesting and reading a packet from the server. The packet is saved in
     * the {@link #packet} StringBuilder. The packet is guaranteed to be complete once this function returns anything
     * except -1. In the case the return value is -1 the connection has probably timed out.
     * <br>
     * <b>NOTE: </b>The returned time must be correctly handled because in the case the connection drops or times out
     * this is the only way to know.
     *
     * @param request_code the request code (ACK or NACK)
     * @return the request starting time in nanoseconds or -1 if there was an error
     */
    private long readPacket(String request_code) {
        int k;  // Each input byte

        long start_time = System.currentTimeMillis();  // Take a time measurement before the packet is sent

        // Request the packet with the code provided as an argument
        if (this.connection.getModem().write(request_code.getBytes())) {

            while (true) {
                try {
                    // Read the next byte
                    k = this.connection.getModem().read();

                } catch (Exception e) {
                    System.out.println("Exception thrown: " + e.toString());

                    // Reset packet line
                    this.packet.setLength(0);
                    return -1;
                }

                // if -1 is read there was an error and the connection timed out
                if (k == -1) {
                    System.out.println("Connection timed out.");  // DEBUG comment??
                    return -1;
                }

                // Append to the packet string
                this.packet.append((char) k);

                // Detect end of echo packet
                if (isTransmissionOver()) {
                    return start_time;
                }
            }
        }
        return -1;
    }

    /**
     * Checks the CheckSum8 Xor for the data included in the packet.
     *
     * @param data_packet the received packet
     * @return true is the checksum matches false if not
     */
    private boolean isCheckSumOk(String data_packet) {
        // Isolate the data and the check sum
        String message = data_packet.split("<")[1].split(" PSTOP")[0];

        // Get the data. WARNING!! substring() upper limit is exclusive
        String data = message.substring(0, 16);

        // Parse the checksum
        int check_sum = Integer.parseInt(message.substring(18));

        // Calculate the check sum
        int XOR = data.charAt(0);
        for (int i = 1; i < data.length(); i++) {
            XOR = XOR ^ data.charAt(i);
        }

        //System.out.println(data);  // Debug comment
        //System.out.println(XOR);  // Debug comment

        // Return the comparison between the calculated check sum and the received one
        return XOR == check_sum;
    }
}
