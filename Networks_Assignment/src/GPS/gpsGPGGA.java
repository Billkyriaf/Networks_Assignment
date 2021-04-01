package GPS;


/**
 * <h1>gpsGPGGA Class</h1>
 * Model Class for GPGGA type GPS data based on the NMEA protocol. The class provides data storage and parsing functions.
 * <br>
 * All the class needs is the String data line received in the {@link GPS.GPSPackets} and then it automatically parses
 * the data in the {@link #gpsGPGGA(String)} class constructor. All the attributes of the class are of type String.
 *
 * @author Vasilis Kyriafinis
 * @version 1.0
 * @since 1.0
 */
public class gpsGPGGA {
    final String message_ID = "$GPGGA";
    String utcTime;
    String latitude;
    String NS_indicator;
    String longitude;
    String EW_indicator;
    String position_fix_indicator;
    String satellites_used;
    String hdop;
    String MSL_altitude;
    String altitude_units;
    String geoid_separation;
    String separation_units;
    String age_of_diff_correction;
    String diff_ref_station_ID = "0000";
    String checksum;

    /**
     * The String line that the received GPS data is stored
     */
    String line;


    /**
     * Class Constructor. The constructor parses the data from the {@link #line} attribute and saves them to the
     * corresponding attribute.
     *
     * <b>Note:</b> In most cases some of the fields of the GPGGA data are not populated by the server. In those cases
     * the corresponding attribute will be and empty String.
     *
     * @param line {@link #line}
     */
    public gpsGPGGA(String line) {
        this.line = line;

        // Split the line on every , and *
        String[] parsed_message = this.line.split(",|\\*");

        if (parsed_message.length == 16) {
            this.utcTime = parsed_message[1].split("\\.")[0];  // keep only the hh mm ss part of the time
            this.latitude = parsed_message[2];
            this.NS_indicator = parsed_message[3];
            this.longitude = parsed_message[4];
            this.EW_indicator = parsed_message[5];
            this.position_fix_indicator = parsed_message[6];
            this.satellites_used = parsed_message[7];
            this.hdop = parsed_message[8];
            this.MSL_altitude = parsed_message[9];
            this.altitude_units = parsed_message[10];
            this.geoid_separation = parsed_message[11];
            this.separation_units = parsed_message[12];
            this.age_of_diff_correction = parsed_message[13];
            this.diff_ref_station_ID = parsed_message[14];
            this.checksum = parsed_message[15];
        } else {
            System.out.println("Error parsing data too many or too few arguments!");
        }
    }

    /**
     * Gets the raw data line String
     *
     * @return {@link #line}
     */
    public String getLine() {
        return this.line;
    }

    /**
     * Gets the coordinates from the {@link #longitude} and {@link #latitude} attributes and transforms the coordinates
     * from Packed DMS with decimal point to Delimited Degrees Minutes Seconds format.
     * See <a href="https://www.avenza.com/help/geographic-imager/5.0/coordinate_formats.htm">Coordinate Formats</a>.
     * <br>
     * The String values are converted to long and the seconds are rounded to the nearest Integer because the server
     * requires all the coordinates be Integers.
     * <br>
     * <b>Note: </b> The Server only accepts latitude degrees from 0° to 99° as it uses only 2 digits for degrees.
     *
     * @return The function returns the String AABBCCDDEEFF where:
     * <ul>
     *     <li>AA: longitude degrees</li>
     *     <li>BB: longitude minutes</li>
     *     <li>CC: longitude seconds</li>
     *     <li>DD: latitude degrees</li>
     *     <li>EE: latitude minutes</li>
     *     <li>FF: latitude seconds</li>
     * </ul>
     */
    public String getCoordinates() {
        // Separate the degrees from minutes and seconds
        String longitude_deg = this.longitude.substring(0, 3);

        // If the longitude is more that 90° we keep only the 2 most significant digits
        if (longitude_deg.startsWith("0")) {
            longitude_deg = longitude_deg.substring(1);
        }

        String longitude_min = this.longitude.substring(3, 5);
        String longitude_sec = "0" + this.longitude.substring(5);

        // Calculate the seconds
        double long_sec = Double.parseDouble(longitude_sec);
        long_sec = long_sec * 60;
        longitude_sec = String.valueOf((int) Math.round(long_sec));

        if (longitude_sec.length() != 2) {
            longitude_sec = "0" + longitude_sec;
        }

        String longitude = longitude_deg + longitude_min + longitude_sec;

        // Separate the degrees from minutes and seconds
        String latitude_deg = this.latitude.substring(0, 2);
        String latitude_min = this.latitude.substring(2, 4);
        String latitude_sec = "0" + this.latitude.substring(4);

        // Calculate the seconds
        double lat_sec = Double.parseDouble(latitude_sec);
        lat_sec = lat_sec * 60;
        latitude_sec = String.valueOf((int) Math.round(lat_sec));

        // If the seconds are less that 10 a 0 is added in front of the number
        if (latitude_sec.length() != 2) {
            latitude_sec = "0" + latitude_sec;
        }

        String latitude = latitude_deg + latitude_min + latitude_sec;

        return longitude + latitude;
    }
}
