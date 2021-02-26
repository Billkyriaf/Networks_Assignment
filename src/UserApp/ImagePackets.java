package UserApp;

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

public class ImagePackets {

    private final Connection connection;
    private String camera_commands;


    /**
     * Constructor
     * @param connection the connection object
     */
    public ImagePackets(Connection connection){
        this.connection = connection;
        this.camera_commands = null;
    }

    /**
     * Constructor
     * @param connection the connection object
     * @param camera_commands special camera commands (direction, size, etc)
     */
    public ImagePackets(Connection connection, String camera_commands){
        this.connection = connection;
        setCamera_commands(camera_commands);
    }

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

    /**
     * Gets images from the server.
     * @param has_errors Indicates if a corrupted image will be fetched
     */
    public void getImage(Boolean has_errors) {
        int k;  // The input buffer byte
        List<Byte> image = new ArrayList<>();
        String request_code;

        Modem modem = this.connection.getModem();

        // Choose from between a request with errors and an error free request
        if (has_errors){
            request_code = this.connection.getImage_code_error();
        }
        else {
            request_code = this.connection.getImage_code();
        }

        // Request the image
        if (modem.write(request_code.getBytes())) {
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
                    image.add((byte)k);

                    // Detect end of image
                    if (isImageComplete(image)) break;

                } catch (Exception x) {
                    System.out.println("Exception thrown: " + x.toString());
                    break;
                }
            }

            System.out.println("Image received\n\n");

            saveImageToFile(image, has_errors);

        } else {
            System.out.println("Failed to send image code");
        }
    }

    /**
     * Compares the last elements of the list with 0xFF 0xD9 jpeg end bytes
     * @param list the data list
     * @return true if all the elements are found false else
     */
    private boolean isImageComplete(List<Byte> list){
        // If the list is smaller than the pattern return false
        if (list.size() < 2) {
            return false;
        } else {
            return list.get(list.size() - 1).equals((byte) 217) && list.get(list.size() - 2).equals((byte) 255);
        }
    }

    /**
     * Gets the current date time and formats it
     * @return Datetime in the form of "yyyy-MM-dd HH-mm-ss"
     */
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

    /**
     * Save an image in the form of a Byte List to a .jpeg file
     * @param image The bytes of the image
     * @param has_errors Is the image corrupted
     */
    private void saveImageToFile(List<Byte> image, Boolean has_errors){
        // Convert the bytes to an Image file
        try {
            // Create an input stream from the bytes of the image
            InputStream is = new ByteArrayInputStream(Bytes.toArray(image));

            String file_name;

            // File to save image
            if (has_errors) {
                file_name = "Received_images/" + "Corrupted_image " + getDate() + ".jpeg";
            }
            else {
                file_name = "Received_images/" + "Clear_image " + getDate() + ".jpeg";
            }

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
}
