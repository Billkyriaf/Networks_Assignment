package UserApp;

import Stracture.DataPackets;
import Stracture.gpsGPGGA;
import Stracture.gpsGPGSA;
import Stracture.gpsGPRMC;
import ithakimodem.Modem;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class GPSPackets implements DataPackets {
    private final Connection connection;
    private final List<gpsGPGGA> gpsGPGGAList;
    private final List<gpsGPGSA> gpsGPGSAList;
    private final List<gpsGPRMC> gpsGPRMCList;

    private final List<String> lines = new ArrayList<>();

    private final StringBuilder gps_line = new StringBuilder();

    private final ImagePackets imagePackets;


    public GPSPackets(Connection connection, ImagePackets imagePackets) {
        this.connection = connection;

        this.gpsGPGGAList = new ArrayList<>();
        this.gpsGPGSAList = new ArrayList<>();
        this.gpsGPRMCList = new ArrayList<>();

        this.imagePackets = imagePackets;
    }


    // ===============================  Interface methods  ===============================

    /**
     * Gets gps packages from the server and saves them to a file
     */
    @Override
    public void getPackets() {
        Modem modem = connection.getModem();

        String request_code = connection.getGps_code();
        int k; // input bytes

        // Request the gps data
        if (modem.write((request_code).getBytes())) {
            System.out.println("Receiving gps data ...");
            while (true) {
                try {
                    // Read the bytes
                    k = modem.read();

                    // if -1 is read there was an error and the connection timed out
                    if (k == -1) {
                        System.out.println("Connection timed out.");
                        break;
                    }

                    // Add chars to the string line
                    this.gps_line.append((char) k);


                    // Detect end of line or end of transmission
                    if (isTransmissionOver()){
                        System.out.print("\n");
                        break;
                    }


                } catch (Exception x) {
                    System.out.println("Exception thrown: " + x.toString());
                    break;
                }
            }

            // System.out.println("GPS data received: " + gps_line.toString()); // DEBUG comment

            // Request visualization images
            parseData();
            getImages(modem, request_code);

            //Save data to file
            saveToFile(createFileName());



        } else {
            System.out.println("Failed to send gps code");
        }
    }

    /**
     * Detects the data delimiters for the GPS transmissions. If an end of line is found the data line is saved in the
     * lines array and the buffer is cleared so that the next line can be saved.
     *
     * @return true if end of transmission is found false else
     */
    @Override
    public boolean isTransmissionOver() {
        String line_end = Commands.GPS_DATA_LINE_END.getStr();
        String transmission_end = Commands.GPS_TRANSMISSION_END.getStr();
        String transmission_start = Commands.GPS_TRANSMISSION_START.getStr();

        String gps_line = this.gps_line.toString();


        if (gps_line.endsWith(transmission_start)){  // Check if this is the transmission start ...
            // Clear the buffer
            this.gps_line.setLength(0);
            return false;
        }
        else if (gps_line.endsWith(transmission_end)){  // ... or the transmission end ...
            return true;
        }
        else if (gps_line.endsWith(line_end)){  // ... or a line end.

            this.lines.add(gps_line);
            this.gps_line.setLength(0);
            return false;
        }
        else {
            return false;
        }

    }

    /**
     * Saves all the gps packets received to a single file and clears all the lists so no duplicates exist
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

            writer.write("GPGGA: " + System.lineSeparator());

            for(gpsGPGGA gpgga: this.gpsGPGGAList) {
                writer.write(gpgga.getLine() + System.lineSeparator());
            }

            writer.write(System.lineSeparator() + "GPGSA: " + System.lineSeparator());

            for(gpsGPGSA gpgsa: this.gpsGPGSAList) {
                writer.write(gpgsa.getLine() + System.lineSeparator());
            }

            writer.write(System.lineSeparator() + "GPRMC: " + System.lineSeparator());

            for(gpsGPRMC gprmc: this.gpsGPRMCList) {
                writer.write(gprmc.getLine() + System.lineSeparator());
            }

            writer.close();

            this.gpsGPGGAList.clear();
            this.gpsGPGSAList.clear();
            this.gpsGPRMCList.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the current date time and formats it "yyyy-MM-dd HH-mm-ss"
     * @return name + date + .txt
     */
    @Override
    public String createFileName() {
        // Create the name of the image file depending on the errors
        String name = "GPS/GPS_Data ";

        String pattern = "yyyy-MM-dd HH-mm-ss";

        // Create an instance of SimpleDateFormat used for formatting
        // the string representation of date according to the chosen pattern
        DateFormat df = new SimpleDateFormat(pattern);

        // Get the today date using Calendar object.
        Date today = Calendar.getInstance().getTime();

        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.

        return name + df.format(today) + ".txt";
    }



    // ===============================  Class methods  ===============================

    /**
     * Categorises data based on the type of the protocol
     */
    private void parseData(){

        for (String line: this.lines) {
            if (line.startsWith(Commands.GPGGA.getStr())){
                // save the GPGGA to the list and clear the buffer
                gpsGPGGA tmp = new gpsGPGGA(line);
                this.gpsGPGGAList.add(tmp);
                this.gps_line.setLength(0);
            }
            else if (line.startsWith(Commands.GPGSA.getStr())){
                // save the GPGSA to the list and clear the buffer
                gpsGPGSA tmp = new gpsGPGSA(line);
                this.gpsGPGSAList.add(tmp);
                this.gps_line.setLength(0);
            }
            else if (line.startsWith(Commands.GPRMC.getStr())){
                // save the GPRMC to the list and clear the buffer
                gpsGPRMC tmp = new gpsGPRMC(line);
                this.gpsGPRMCList.add(tmp);
                this.gps_line.setLength(0);
            }
        }
    }

    /**
     * Gets visualized images for the gps data
     * @param modem the modem of the connection
     */
    private void getImages(Modem modem, String request_code){
        int k;

        // request data visualization
        for (gpsGPGGA data: this.gpsGPGGAList) {
            String tParam = data.getCoordinates();

            if (modem.write((request_code + "T=" + tParam).getBytes())) {
                System.out.println("Receiving gps image data ...");
                while (true) {
                    try {
                        // Read the bytes
                        k = modem.read();

                        // if -1 is read there was an error and the connection timed out
                        if (k == -1) {
                            System.out.println("Connection timed out.");
                            break;
                        }

                        System.out.print((byte)k + " ");

                        // Add bytes to the image Byte List
                        this.imagePackets.addToImageList((byte) k);


                        // Detect end of line or end of transmission
                        if (this.imagePackets.isTransmissionOver()) {
                            String name = createFileName().split("\\.")[0] + ".jpeg";

                            // Build the new name GPS/GPS Images/GPS_Data yyyy-MM-dd HH-mm-ss.jpeg
                            name = name.substring(0, 4) + "GPS Images/" + name.substring(4);

                            // Save the image to a file
                            this.imagePackets.saveToFile(name);
                            break;
                        }


                    } catch (Exception x) {
                        System.out.println("Exception thrown: " + x.toString());
                        break;
                    }
                }
            } else {
                System.out.println("Failed to send gps code");
            }
        }
    }
}
