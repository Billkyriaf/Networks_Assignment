package Structure;

import ithakimodem.Modem;

/**
 * The connection class handles the initial connection to the server and the connection mode selection.
 *
 * @author Vasilis Kyriafinis
 * @version 1.0
 * @since 1.0
 */
public class Connection {

    private Modem modem;
    private final String echo_code;
    private String image_code;
    private String image_code_error;
    private String gps_code;
    private final String ack_code;
    private final String nack_code;

    /**
     * Class constructor. Creates the modem object that is used for the connections
     *
     * @param echo      Echo request code   : E_XXXX
     * @param image     Image request code  : M_XXXX  (Tx/Rx error free)
     * @param image_err Image request code  : G_XXXX  (Tx/Rx with errors)
     * @param gps       GPS request code    : P_XXXX
     * @param ack       ACK result code     : Q_XXXX
     * @param nack      NACK result code    : R_XXXX
     * @param speed     Speed of the connection
     * @param timeout   Timeout time in seconds for the connection
     */
    public Connection(String echo, String image, String image_err, String gps, String ack, String nack, int speed, int timeout) {
        // Create a new Modem object
        this.modem = new Modem();

        //Setup the modem for initial connection test
        modem.setSpeed(speed);  // Connection speed
        modem.setTimeout(timeout);  // The timeout time. After this time with no activity the connection times out

        // Save all the temporary codes from the command line arguments
        this.echo_code = echo + '\r';
        this.image_code = image + '\r';
        this.image_code_error = image_err + '\r';
        this.gps_code = gps + '\r';
        this.ack_code = ack + '\r';
        this.nack_code = nack + '\r';

        // Start the data connection with the server
        if (startDataConnection()) {
            System.out.println("Connection successful!");
        } else {
            System.out.println("Connection failed. Terminating...");

            // Close the connection
            this.modem.close();

            // Exit program
            System.exit(-1);
        }
    }

    // Getters Setters
    public Modem getModem() {
        return modem;
    }

    public String getEcho_code() {
        return echo_code;
    }

    public String getImage_code() {
        return image_code;
    }

    public void setImage_code(String code) {
        this.image_code = code;
    }

    public String getImage_code_error() {
        return image_code_error;
    }

    public void setImage_code_error(String image_code_error) {
        this.image_code_error = image_code_error;
    }

    public String getGps_code() {
        return gps_code;
    }

    public String getAck_code() {
        return ack_code;
    }

    public String getNack_code() {
        return nack_code;
    }

    public void setModemSpeed(int speed){
        this.modem.setSpeed(speed);
    }

    public void setModemTimeout(int timeout){
        this.modem.setTimeout(timeout);
    }

    /**
     * Starts the data connection with the server
     *
     * @return True if the connection is successful false else
     */
    private boolean startDataConnection() {
        int k;  // The input buffer byte
        StringBuilder packet = new StringBuilder();  // Complete packet

        // Start the connection to data mode
        if (!this.modem.open(Constants.DATA_MODE.getStr())) {
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
                    if (isMessageOver(packet.toString(), Constants.SM_END.getStr())) break;

                } catch (Exception x) {
                    System.out.println("Exception thrown: " + x.toString());

                    return false;
                }
            }

            System.out.println(packet.toString());
            return true;
        }
    }

    /**
     * The reconnect function re establishes the connection with the server if the server
     *
     * @param speed   Speed of the connection
     * @param timeout Timeout time in seconds for the connection
     * @return If the connection is successful the function returns true else it returns false
     */
    public boolean reconnect(int speed, int timeout) {
        this.modem = null;

        // Create a new Modem object
        this.modem = new Modem();

        //Setup the modem for initial connection test
        modem.setSpeed(speed);  // Connection speed
        modem.setTimeout(timeout);  // The timeout time. After this time with no activity the connection times out

        // Start the data connection with the server
        if (startDataConnection()) {
            System.out.println("Connection successful");
            return true;
        } else {
            System.out.println("Connection failed");

            // Close the connection
            try {
                this.modem.close();
            } catch (Exception e) {
                System.out.println(e.toString());
            }

            return false;
        }
    }

    /**
     * Compares a message's end with a pattern string
     *
     * @param message the message that is received
     * @param pattern the patter that needs to be found to the end of the message
     * @return True if the pattern matched false if not or if the message was too short
     */
    public boolean isMessageOver(String message, String pattern) {
        if (message.length() < pattern.length())
            return false;
        else
            return message.endsWith(pattern);
    }
}
