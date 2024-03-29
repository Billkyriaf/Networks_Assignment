package GPS;

import Structure.DataPackets;
import Structure.Constants;
import Structure.Connection;
import Image.ImagePackets;
import ithakimodem.Modem;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * <h1>gpsGPSPackets Class</h1>
 * GPSPackets is the class that handles all the actions regarding the GPS data requested from the server. The server
 * provides 3 types of GPS NMEA data protocols:
 * <br>
 * <ul>
 *     <li>The GPGGA protocol {@link GPS.gpsGPGGA}</li>
 *     <li>The GPGSA protocol {@link GPS.gpsGPGSA}</li>
 *     <li>And the GPRMC protocol {@link GPS.gpsGPRMC}</li>
 * </ul>
 * <br>
 *     Also upon special request satellite images fro the google maps are provided with marked coordinate points.
 * <br>
 *     Finally the class implements the {@link Structure.DataPackets} interface witch provides the basic structure for
 *     the class.
 *
 * @author Vasilis Kyriafinis
 * @version 1.0
 * @since 1.0
 */
public class GPSPackets implements DataPackets {
    /**
     * The {@link Structure.Connection} instance of the server connection
     */
    private final Connection connection;

    /**
     * This code is used to request from the server specific saved routes.
     * <br>
     * Format: XPPPPLL
     * <ul>
     *     <li>X: Number of the route 0 - 9</li>
     *     <li>PPPP: Starting Point</li>
     *     <li>LL: Number of data points 0 - 99</li>
     * </ul>
     */
    private String gpsLLCode = "";

    /**
     * Holds the utc time of the last gps data point
     */
    private String lastDataPointTime = "000000";

    /**
     * A List of all the {@link GPS.gpsGPGGA} lines received
     */
    private final List<gpsGPGGA> gpsGPGGAList;

    /**
     * A List of all the {@link GPS.gpsGPGSA} lines received
     */
    private final List<gpsGPGSA> gpsGPGSAList;

    /**
     * A List of all the {@link GPS.gpsGPRMC} lines received
     */
    private final List<gpsGPRMC> gpsGPRMCList;

    /**
     * A List of all the objects used for gps image data visualization. It must be ensured that the data saved here are
     * at least 4 seconds apart.
     */
    private final List<gpsGPGGA> imageDataList;

    /**
     * A list that holds all the String lines received from the server before they are transformed to one of the
     * protocols objects.
     */
    private final List<String> lines = new ArrayList<>();

    /**
     * This StringBuilder is used to construct the received line Byte by Byte
     */
    private final StringBuilder gps_line = new StringBuilder();

    /**
     * An instance of the {@link Image.ImagePackets} class. This instance is used to process the images with the visualized
     * gps locations from the server.
     */
    private final ImagePackets imagePackets;


    /**
     * Constructor of the class with gps_code LL parameters initialization.
     *
     * @param connection {@link #connection}
     * @param gpsLLCode  {@link #gpsLLCode} pass empty string if not used
     */
    public GPSPackets(Connection connection, String gpsLLCode) {
        this.connection = connection;

        if (!gpsLLCode.isEmpty())
            this.gpsLLCode = gpsLLCode;

        this.gpsGPGGAList = new ArrayList<>();
        this.gpsGPGSAList = new ArrayList<>();
        this.gpsGPRMCList = new ArrayList<>();
        this.imageDataList = new ArrayList<>();

        // Init the image processing object
        this.imagePackets = new ImagePackets(this.connection, false);
    }

    /**
     * Default constructor.
     *
     * @param connection {@link #connection}
     */
    public GPSPackets(Connection connection) {
        this.connection = connection;

        this.gpsGPGGAList = new ArrayList<>();
        this.gpsGPGSAList = new ArrayList<>();
        this.gpsGPRMCList = new ArrayList<>();
        this.imageDataList = new ArrayList<>();

        // Init the image processing object
        this.imagePackets = new ImagePackets(this.connection, false);
    }


    /**
     * Requests gps data packages from the server. The packets are based on the NMEA protocol format.
     * <br>
     * The start of transmission is always {@link Structure.Constants#GPS_TRANSMISSION_START} and the end of
     * transmission is always {@link Structure.Constants#GPS_TRANSMISSION_END}. The data lines always terminate with
     * {@link Structure.Constants#GPS_DATA_LINE_END}.
     * <br>
     * After the transmission is over and the data are saved the server provides the choice to request for visualization
     * of the received locations. This is possible at maximum 9 times for each data request made and it is handled by
     * {@link #getImages(Modem, String)} function.
     * <br>
     * <b>Note:</b> Before requesting location visualisation or saving data to a file the received data must be
     * categorised by {@link #parseData()} function.
     */
    @Override
    public void getPackets() {
        Modem modem = connection.getModem();

        String request_code = connection.getGps_code();
        int k; // input bytes

        if (!this.gpsLLCode.isEmpty())
            request_code = request_code.substring(0, 5) + this.gpsLLCode + "\r";

        // This outer loop serves the purpose of requesting data again if a connection drops. The loop allows up to 3
        // tries to recover from a dropped connection. After that the program will exit.
        for (int i = 0; i <= 3; i++) {
            // Request the gps data
            if (modem.write((request_code).getBytes())) {
                System.out.println("Receiving gps data ...");
                while (true) {
                    try {
                        // Read the bytes
                        k = modem.read();
                    } catch (Exception e) {
                        // If any exception is thrown here we exit the program because something has gone wrong and there
                        // is no chance of recovering.
                        System.out.println("Exception thrown: " + e.toString());

                        System.out.println("Failed to receive gps packets. Terminating...");
                        return;
                    }

                    // if -1 is read there was an error and the connection timed out
                    if (k == -1) {

                        // if -1 is read there was an error and the connection timed out or dropped unexpectedly
                        System.out.println("Connection timed out. Reconnecting...");

                        // Try to reconnect with the server
                        if (this.connection.reconnect(80000, 10000)) {
                            // Drop the incomplete packet
                            this.gps_line.setLength(0);

                            // Clear the old data to receive to new
                            this.lines.clear();

                            System.out.println("Reconnected successfully. Continuing...");
                            break;
                        } else {
                            System.out.println("Reconnection failed. Check if the codes have expired!!");
                            return;
                        }
                    }

                    // Add chars to the string line
                    this.gps_line.append((char) k);


                    // Detect end of line or end of transmission
                    if (isTransmissionOver()) {
                        System.out.println("Data received");
                        // System.out.println("GPS data received: " + gps_line.toString()); // debug comment

                        // Categorise the received data
                        parseData();

                        // Save data to file
                        saveToFile(createFileName(Constants.GPS_DATA_DIR.getStr(), ".txt"));

                        break;
                    }


                }

                if (!this.lines.isEmpty()) {
                    // if there are data saved brake from the outer loop else request data again
                    // (possibly the connection dropped and the program reconnected)
                    break;
                }

            } else {
                System.out.println("Unrecoverable exception occurred while receiving GPS data. Terminating...");
                return;
            }
        }

        // If the for loop exits and no data are saved we exit the function
        if (this.lines.isEmpty()) {
            System.out.println("Detected multiple connection fails. Terminating...");
            return;
        }

        // Request visualization images
        getImages(modem, request_code);
    }

    /**
     * Compares the packet's (at the current state) end with the {@link Structure.Constants#GPS_TRANSMISSION_START},
     * {@link Structure.Constants#GPS_TRANSMISSION_END} and {@link Structure.Constants#GPS_DATA_LINE_END} strings.
     *
     * @return <ul>
     * <li>If {@link Structure.Constants#GPS_TRANSMISSION_START} is matched the function returns false.</li>
     * <li>If {@link Structure.Constants#GPS_TRANSMISSION_END} is matched the function returns true.</li>
     * <li>If {@link Structure.Constants#GPS_DATA_LINE_END} is matched the function returns false.</li>
     * </ul>
     */
    @Override
    public boolean isTransmissionOver() {
        String line_end = Constants.GPS_DATA_LINE_END.getStr();
        String transmission_end = Constants.GPS_TRANSMISSION_END.getStr();
        String transmission_start = Constants.GPS_TRANSMISSION_START.getStr();

        String gps_line = this.gps_line.toString();


        if (gps_line.endsWith(transmission_start)) {  // Check if this is the transmission start ...
            // Clear the buffer from the useless starting message
            this.gps_line.setLength(0);
            return false;
        } else if (gps_line.endsWith(transmission_end)) {  // ... or the transmission end ...
            return true;
        } else if (gps_line.endsWith(line_end)) {  // ... or a line end.

            // Save the line
            this.lines.add(gps_line);

            // Reset the buffer so the new line will be written
            this.gps_line.setLength(0);
            return false;
        } else {
            return false;
        }
    }

    /**
     * Save all the GPS packets form the Lists to a file. The file starts with ## request_codes ##
     * for later identification. Every NMEA protocol is saved under the corresponding title eg GPGGA.
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
            writer.write("####\n" + this.connection.getEcho_code() + this.connection.getImage_code() +
                    this.connection.getImage_code_error() + this.connection.getGps_code() +
                    this.connection.getAck_code() + this.connection.getNack_code() + "###" +
                    System.lineSeparator());

            writer.write("GPGGA: " + System.lineSeparator());

            // Write the GPGGA data
            for (gpsGPGGA gpgga : this.gpsGPGGAList) {
                writer.write(gpgga.getLine() + System.lineSeparator());
            }

            writer.write(System.lineSeparator() + "GPGSA: " + System.lineSeparator());

            // Write the GPGSA data
            for (gpsGPGSA gpgsa : this.gpsGPGSAList) {
                writer.write(gpgsa.getLine() + System.lineSeparator());
            }

            writer.write(System.lineSeparator() + "GPRMC: " + System.lineSeparator());

            // Write the GPRMC data
            for (gpsGPRMC gprmc : this.gpsGPRMCList) {
                writer.write(gprmc.getLine() + System.lineSeparator());
            }

            writer.close();

            // Clear the lists so that no duplicate data are saved
            this.gpsGPGGAList.clear();
            this.gpsGPGSAList.clear();
            this.gpsGPRMCList.clear();

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
     * @param directory      The directory the file will be saved.
     * @param file_extension The type of the file e.g.  .txt
     * @return directory + name + date + file_extension
     */
    @Override
    public String createFileName(String directory, String file_extension) {
        // Create the name of the image file depending on the errors

        String name = "GPS_Data ";

        String pattern = "yyyy-MM-dd HH-mm-ss";

        // Create an instance of SimpleDateFormat used for formatting
        // the string representation of date according to the chosen pattern
        DateFormat df = new SimpleDateFormat(pattern);

        // Get the today date using Calendar object.
        Date today = Calendar.getInstance().getTime();

        name = name + df.format(today) + file_extension;

        return directory + name;
    }


    /**
     * Categorises data in the corresponding Lists based on the type of the protocol used. If the protocol is GPGGA the
     * time difference between the last GPGGA data point and the current data point is calculated with
     * {@link #timeDifference(String current_data_point_time)}. If the difference is more than a value and the
     * {@link #imageDataList} has less than 9 data points the current data point is added to the list.
     */
    private void parseData() {

        for (String line : this.lines) {
            if (line.startsWith(Constants.GPGGA.getStr())) {
                // save the GPGGA to the list and clear the buffer
                gpsGPGGA tmp = new gpsGPGGA(line);
                this.gpsGPGGAList.add(tmp);
                this.gps_line.setLength(0);

                // check the time of the gpgga data. If 10 seconds have passed add the Data to the imageDataList for visualization.
                if (timeDifference(tmp.utcTime) >= 10 && this.imageDataList.size() <= 9) {
                    this.imageDataList.add(tmp);
                    // Update the lastDataPointTime
                    this.lastDataPointTime = tmp.utcTime;
                }

            } else if (line.startsWith(Constants.GPGSA.getStr())) {
                // save the GPGSA to the list and clear the buffer
                gpsGPGSA tmp = new gpsGPGSA(line);
                this.gpsGPGSAList.add(tmp);
                this.gps_line.setLength(0);
            } else if (line.startsWith(Constants.GPRMC.getStr())) {
                // save the GPRMC to the list and clear the buffer
                gpsGPRMC tmp = new gpsGPRMC(line);
                this.gpsGPRMCList.add(tmp);
                this.gps_line.setLength(0);
            }
        }
    }

    /**
     * Gets visualized images for the locations received. The function makes use of the {@link Image.ImagePackets} class
     * functionality by using the {@link #imagePackets} attribute.
     * <br>
     * The coordinates of the location are passed in the T parameter after the gps_request_code in the format
     * gps_request_codeT=AABBCCDDEEFF\r
     *
     * @param modem        the modem of the {@link #connection}
     * @param request_code the gps data request code
     */
    private void getImages(Modem modem, String request_code) {
        int k;
        StringBuilder request = new StringBuilder(request_code.substring(0, 5));

        if (this.imageDataList.isEmpty()) {
            System.out.println("Could not find data for image request. Terminating...");
            return;
        }

        // For each entry found in the gpsGPGGAList ...
        for (gpsGPGGA data : this.imageDataList) {
            // ... get the coordinates
            String coordinates = data.getCoordinates();

            //System.out.println(coordinates);  // debug comment

            // Build the request code
            request.append("T=").append(coordinates);
        }

        // finally finish the request with a \r
        request.append("\r");

        //System.out.println(request.toString());  // debug comment

        // Request the data
        if (modem.write(request.toString().getBytes())) {
            System.out.println("Receiving gps image data ...");
            while (true) {
                try {
                    // Read the bytes
                    k = modem.read();

                    // if -1 is read there was an error and the connection timed out
                    if (k == -1) {
                        System.out.println("Connection timed out. Terminating...");
                        return;
                    }

                    // Add bytes to the image Byte List
                    this.imagePackets.addToImageList((byte) k);

                    // Detect end of line or end of transmission
                    if (this.imagePackets.isTransmissionOver()) {
                        // Name of the file
                        String fileName = createFileName(Constants.GPS_IMAGES_DIR.getStr(), ".jpeg");

                        // Save the image to a file
                        this.imagePackets.saveToFile(fileName);

                        // Clear the list so that the next image can be saved
                        this.imagePackets.clearImageList();
                        break;
                    }


                } catch (Exception e) {
                    System.out.println("Unrecoverable exception occurred: " + e.toString() + " Terminating...");
                    return;
                }
            }
        } else {
            System.out.println("Failed to send gps code");
        }
    }

    /**
     * Finds the difference in seconds between the time param and the {@link #lastDataPointTime}
     *
     * @param time the time to compare. Must be in format hhmmss String
     * @return the difference in seconds
     */
    private int timeDifference(String time) {
        // Parse the time of the last gps packet received and convert it in seconds
        int last_point_h = Integer.parseInt(this.lastDataPointTime.substring(0, 2)) * 3600;
        int last_point_m = Integer.parseInt(this.lastDataPointTime.substring(2, 4)) * 60;
        int last_point_s = Integer.parseInt(this.lastDataPointTime.substring(4, 6));

        last_point_s += last_point_h + last_point_m;

        // Parse the time of the current gps packet and convert it in seconds
        int h = Integer.parseInt(time.substring(0, 2)) * 3600;
        int m = Integer.parseInt(time.substring(2, 4)) * 60;
        int s = Integer.parseInt(time.substring(4, 6));

        s += h + m;

        return s - last_point_s;
    }
}
