package GPS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * <h1>gpsGPGSA Class</h1>
 * Model Class for GPGGA type GPS data based on the NMEA protocol. The class provides data storage.
 * <br>
 * All the class needs is the String data line received in the {@link GPS.GPSPackets} and then it automatically parses
 * the data in the {@link #gpsGPGSA(String)} class constructor. All the attributes of the class are of type String.
 *
 * @author Vasilis Kyriafinis
 * @version 1.0
 * @since 1.0
 */
public class gpsGPGSA {
    final String message_ID = "$GPGSA";
    String mode_1;
    String mode_2;

    /**
     * The GPGSA protocol provides all the satellites used for the data provided. The satellites list stores the number
     * of each satellite used in String format.
     * <br>
     *     <b>Note: </b> If a satellite is used the number of the satellite is stored if a satellite is not used an
     *     empty String is stored.
     *
     */
    List<String> satellites;
    String pdop;
    String hdop;
    String vdop;
    String checksum;

    /**
     * The String line that the received GPS data is stored
     */
    String line;

    /**
     * Class Constructor. The constructor parses the data from the {@link #line} attribute and saves them to the
     * corresponding attribute.
     *
     * <b>Note:</b> In most cases some of the fields of the GPGSA data are not populated by the server. In those cases
     * the corresponding attribute will be and empty String.
     *
     * @param line {@link #line}
     */
    public gpsGPGSA(String line){
        this.line = line;
        this.satellites = new ArrayList<>();

        // Split the line on every , and *
        String[] parsed_message = this.line.split(",|\\*");

        if (parsed_message.length == 19) {
            this.mode_1 = parsed_message[1];
            this.mode_2 = parsed_message[2];
            this.satellites.addAll(Arrays.asList(parsed_message).subList(3, 15));
            this.pdop = parsed_message[15];
            this.hdop = parsed_message[16];
            this.vdop = parsed_message[17];
            this.checksum = parsed_message[18];
        }
        else {
            System.out.println("Error parsing data too many or too few arguments!");
        }
    }

    /**
     * Gets the raw data line String
     * @return {@link #line}
     */
    public String getLine() {
        return this.line;
    }
}
