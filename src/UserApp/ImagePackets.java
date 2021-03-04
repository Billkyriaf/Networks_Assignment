package UserApp;

import Stracture.DataPackets;
import com.google.common.primitives.Bytes;
import ithakimodem.Modem;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ImagePackets implements DataPackets {

    private final Connection connection;

    private String camera_commands;  // Camera moving commands (U, D, etc)
    private boolean has_errors;  // Indicates if the images requested have errors

    private final List<Byte> image;  // List with all the bytes of the image


    /**
     * Constructor
     * @param connection the connection object
     * @param has_errors determines if the requested image will have errors or it will be clear
     */
    public ImagePackets(Connection connection, boolean has_errors){
        this.connection = connection;
        this.camera_commands = null;
        this.image = new ArrayList<>();
        this.has_errors = has_errors;
    }

    /**
     * Constructor
     * @param connection the connection object
     * @param camera_commands special camera commands (direction, size, etc)
     * @param has_errors determines if the requested image will have errors or it will be clear
     */
    public ImagePackets(Connection connection, String camera_commands, boolean has_errors){
        this.connection = connection;
        setCamera_commands(camera_commands);
        this.image = new ArrayList<>();
        this.has_errors = has_errors;
    }



    // ===============================  Getters - Setters  ===============================

    /**
     * Sets this.camera_command and updates the values in the Connection object
     * @param camera_commands the extra camera parameters
     */
    public void setCamera_commands(String camera_commands) {
        // Set the initial value
        this.camera_commands = camera_commands;


        // Every time the camera mode is changed Connection codes are updated
        String image_code = this.connection.getImage_code();
        String image_code_error = this.connection.getImage_code_error();


        // remove the \r from the end
        image_code = image_code.substring(0, image_code.length() - 1);
        image_code_error = image_code_error.substring(0, image_code_error.length() - 1);

        // Concatenate the command to the string
        image_code = image_code + this.camera_commands + "\r";
        image_code_error = image_code_error + this.camera_commands + "\r";


        this.connection.setImage_code(image_code);
        this.connection.setImage_code_error(image_code_error);
    }

    public void setHas_errors(boolean has_errors){
        this.has_errors = has_errors;
    }

    public boolean getHas_errors(){
        return this.has_errors;
    }

    public void addToImageList(byte k){
        this.image.add(k);
    }

    public void clearImageList(){
        this.image.clear();
    }

    // ===============================  Interface methods  ===============================

    /**
     * Gets images from the server and saves them to a file
     */
    @Override
    public void getPackets() {
        int k;  // The input buffer byte
        String request_code;  // The request code for the image

        Modem modem = this.connection.getModem();

        // Choose from between a request with errors and an error free request
        if (this.has_errors){
            request_code = this.connection.getImage_code_error();
        }
        else {
            request_code = this.connection.getImage_code();
        }

        // Request the image
        if (modem.write(request_code.getBytes())) {
            System.out.println("Receiving image ...");
            while (true) {
                try {
                    // Read the bytes
                    k = modem.read();

                    // if -1 is read there was an error and the connection timed out
                    if (k == -1) {
                        System.out.println("Connection timed out.");
                        break;
                    }

                    // Add bytes to the image Byte List
                    this.image.add((byte)k);

                    // Detect end of image
                    if (isTransmissionOver()){
                        // Finally the image to the file
                        saveToFile(createFileName());
                        this.image.clear();
                        break;
                    }

                } catch (Exception x) {
                    System.out.println("Exception thrown: " + x.toString());
                    break;
                }
            }

            System.out.println("Image received\n\n");

        } else {
            System.out.println("Failed to send image code");
        }
    }

    /**
     * Compares the last elements of the list with 0xFF 0xD9 jpeg end bytes
     * @return true if all the elements are found false else
     */
    @Override
    public boolean isTransmissionOver() {
        // If the list is smaller than the pattern return false
        if (this.image.size() < 2) {
            return false;
        } else {
            return this.image.get(this.image.size() - 1).equals((byte) 217) && this.image.get(this.image.size() - 2).equals((byte) 255);
        }
    }

    /**
     * Save an image in the form of a Byte List to a .jpeg file
     * @param file_name the name of the file
     */
    @Override
    public void saveToFile(String file_name) {
        // Convert the bytes to an Image file
        try {
            // Create an input stream from the bytes of the image
            InputStream is = new ByteArrayInputStream(Bytes.toArray(this.image));

            FileOutputStream out = new FileOutputStream(file_name);

            // Bytes are read from the stream in 'packs' of 1024
            byte[] data = new byte[1024];
            int readBytes;

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
    }

    /**
     * Gets the current date time and formats it "yyyy-MM-dd HH-mm-ss"
     * @return name + date + .jpeg
     */
    @Override
    public String createFileName() {
        // Create the name of the image file depending on the errors
        String name = has_errors ? "Images/Corrupted_image " : "Images/Clear_image ";

        String pattern = "yyyy-MM-dd HH-mm-ss";

        // Create an instance of SimpleDateFormat used for formatting
        // the string representation of date according to the chosen pattern
        DateFormat df = new SimpleDateFormat(pattern);

        // Get the today date using Calendar object.
        Date today = Calendar.getInstance().getTime();

        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.

        return name + df.format(today) + ".jpeg";
    }
}
