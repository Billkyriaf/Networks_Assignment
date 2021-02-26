package UserApp;

import ithakimodem.Modem;


import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.common.primitives.Bytes;

/**
 * This class provides functions for all the types of connections needed with the server
 */
public class Tasks {

    //TODO getters setters
    Modem modem;
    String echo_code;
    String image_code;
    String image_code_error;
    String gps_code;
    String ack_result_code;
    String nack_result_code;
    private final String server_message_end_sequence = "\r\n\n\n";
    private final String packet_end_sequence = "PSTOP";
    private final String at_end_sequence = "\r\n";
    private final List<Byte> image_end = new ArrayList<>(){
        {
            add((byte)255);
            add((byte)217);
        }
    };
    private boolean connection_started = false;
    List<String> echo_packets;


    /**
     * Class constructor. Creates the modem object that is used for the connections
     *
     * @param echo      Echo request code   : E_XXXX
     * @param image     Image request code  : M_XXXX  (Tx/Rx error free)
     * @param image_err Image request code  : G_XXXX  (Tx/Rx with errors)
     * @param gps       GPS request code    : P_XXXX
     * @param ack       ACK result code     : Q_XXXX
     * @param nack      NACK result code    : R_XXXX
     */
    public Tasks(String echo, String image, String image_err, String gps, String ack, String nack) {
        // Create a new Modem object
        this.modem = new Modem();

        //Setup the modem for initial connection test
        modem.setSpeed(76000);  // Connection speed
        modem.setTimeout(5000);  // The timeout time. After this time with no activity the connection times out

        // Save all the temporary codes from the command line arguments
        this.echo_code = echo + '\r';
        this.image_code = image + '\r';  // + "CAM=PTZ"
        this.image_code_error = image_err + '\r';
        this.gps_code = gps + '\r';
        this.ack_result_code = ack + '\r';
        this.nack_result_code = nack + '\r';

        // initialize the echo packets list
        this.echo_packets = new ArrayList<>();

    }

    /**
     * Test the connection with AT commands
     */
    public void testConnection() {
        // TODO not implemented
        StringBuilder packet = new StringBuilder();  // Complete packet

        if (this.modem.write("at\r".getBytes())) {
            int k;

            while (true) {
                try {
                    // Read from input stream
                    k = this.modem.read();

                    // Check for errors
                    if (k == -1) {
                        System.out.println("Connection timed out.");
                        break;
                    }

                    // Build the message
                    packet.append((char) k);

                    // Check for termination sequence
                    if (pattern_match(packet.toString(), this.at_end_sequence)) {
                        break;
                    }
                } catch (Exception x) {
                    System.out.println("Exception caught" + x.toString());
                    break;
                }
            }

            System.out.println(packet.toString());

        } else {
            System.out.println("Failed to send command");
        }
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
     * @return True if no errors false else
     */
    public boolean getEchoPackets(int packet_counter) {
        int k;  // The input buffer byte
        StringBuilder packet = new StringBuilder();  // Complete packet

        // Check if the data connection is started and if not start it
        if (!this.connection_started) {
            if (!startDataConnection()) return false;
            else this.connection_started = true;
        }

        for (int i = 0; i <= packet_counter; i++) {
            // Request the echo_packet
            if (this.modem.write(this.echo_code.getBytes())) {
                while (true) {
                    try {
                        // Read the bytes
                        k = this.modem.read();

                        // if -1 is read there was an error and the connection timed out
                        if (k == -1) {
                            System.out.println("Connection timed out.");
                            return false;
                        }

                        // Append to the packet string
                        packet.append((char) k);

                        // Detect end of packet
                        if (pattern_match(packet.toString(), this.packet_end_sequence)) break;

                    } catch (Exception x) {
                        System.out.println("Exception thrown: " + x.toString());
                        return false;
                    }
                }

                System.out.println("Packet received: " + packet.toString());

                // Add packet to the list
                this.echo_packets.add(packet.toString());

                // Reset packet
                packet.setLength(0);

            } else {
                System.out.println("Failed to send echo code");

                return false;
            }
        }

        return true;
    }


    public boolean getImages() {
        int k;  // The input buffer byte

        // Check if the data connection is started and if not start it
        if (!this.connection_started) {
            if (startDataConnection()) this.connection_started = true;
            else return false;
        }

        List<Byte> image = new ArrayList<Byte>();

        // Request the image
        if (this.modem.write(this.image_code.getBytes())) {
            while (true) {
                try {
                    // Read the bytes
                    k = this.modem.read();

                    // if -1 is read there was an error and the connection timed out
                    if (k == -1) {
                        System.out.println(k);
                        System.out.println("Connection timed out.");
                        break;
                    }

                    // Add bytes to the image Byte List
                    image.add((byte)k);

                    // Detect end of image
                    if (pattern_match(image, this.image_end)) break;

                } catch (Exception x) {
                    System.out.println("Exception thrown: " + x.toString());
                    return false;
                }
            }

            System.out.println("Image received\n\n");

            // Convert the bytes to an Image
            try {
                // Create an input stream from the bytes of the image
                InputStream is = new ByteArrayInputStream(Bytes.toArray(image));

                // File to save image
                String file_name = "Received_images/" + getDate() + ".jpeg";

                FileOutputStream out = new FileOutputStream(file_name);

                // Bytes are read from the stream in 'packs' of 1024
                byte[] data = new byte[1024];
                int readBytes = 0;

                // Write bytes to file
                while ((readBytes = is.read(data, 0, 1024)) > 0) {
                    out.write(data, 0, readBytes);
                }

                // Close all the streams
                out.flush();
                out.close();
                is.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("Failed to send image code");

            return false;
        }
        return true;
    }

    /**
     * Compares a message's end with a pattern string
     * @param message the message that is received
     * @param pattern the patter that needs to be found to the end of the message
     * @return True if the pattern matched false if not or if the message was too short
     */
    private boolean pattern_match(String message, String pattern) {
        if (message.length() < pattern.length()) {
            return false;
        } else return message.endsWith(pattern);
    }

    /**
     * Compares the last elements of the list with all the elements of the pattern
     * @param list the data list
     * @param pattern the pattern list
     * @return true if all the elements are found false else
     */
    private boolean pattern_match(List<Byte> list, List<Byte> pattern){
        // If the list is smaller than the pattern return false
        if (list.size() < pattern.size()) {
            return false;
        } else {
            // Compare the last pattern.size() elements of the list with all the pattern elements
            for (int i = list.size() - pattern.size(), j = 0; i < list.size(); i++, j++){
                if (!list.get(i).equals(pattern.get(j))){
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Starts the data connection with the server
     * @return True if the connection is successful false else
     */
    public boolean startDataConnection(){
        int k;  // The input buffer byte
        StringBuilder packet = new StringBuilder();  // Complete packet

        // Start the connection to data mode
        if (!this.modem.open(Commands.DATA_MODE.getStr())) {
            // If the connection fails return false
            System.out.println("Failed to enter data mode!");

            return false;

        } else {
            while (true) {
                try {
                    // Read the initial byte
                    k = this.modem.read();

                    // Check for errors
                    if (k == -1) {
                        System.out.println("Connection timed out.");
                        break;
                    }

                    // Build message
                    packet.append((char) k);

                    // Detect end of transmission
                    if (pattern_match(packet.toString(), this.server_message_end_sequence)) break;

                } catch (Exception x) {
                    System.out.println("Exception thrown: " + x.toString());

                    return false;
                }
            }

            System.out.println(packet.toString());
            return true;
        }
    }

    private String getDate(){
        String pattern = "yyyy-MM-dd HH-mm-ss";

        // Create an instance of SimpleDateFormat used for formatting
        // the string representation of date according to the chosen pattern
        DateFormat df = new SimpleDateFormat(pattern);

        // Get the today date using Calendar object.
        Date today = Calendar.getInstance().getTime();

        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.

        return df.format(today);
    }

}
