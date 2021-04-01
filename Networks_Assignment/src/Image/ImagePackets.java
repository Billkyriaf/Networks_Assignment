package Image;

import Structure.Constants;
import Structure.DataPackets;
import Structure.Connection;
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

/**
 * <h1>ImagePackets Class</h1>
 * ImagePackets is the class that handles all the actions regarding the images requested from the server. The server
 * provides 2 types of images. One without any errors and one in witch random errors are inserted in half of the image
 * from the server.
 * <br>
 * The class implements the {@link Structure.DataPackets} interface witch provides the basic structure for the class.
 *
 * @author Vasilis Kyriafinis
 * @version 1.0
 * @since 1.0
 */
public class ImagePackets implements DataPackets {
    /**
     * The {@link Structure.Connection} instance of the server connection
     */
    private final Connection connection;

    /**
     * This string stores the request codes for moving the camera (U, D, etc)
     */
    private String camera_commands;

    /**
     * Indicates if the images requested have errors
     */
    private boolean has_errors;

    /**
     * A List that temporarily holds the image bytes
     */
    private final List<Byte> image;


    /**
     * Constructor
     *
     * @param connection the connection object
     * @param has_errors determines if the requested image will have errors or it will be clear
     */
    public ImagePackets(Connection connection, boolean has_errors) {
        this.connection = connection;
        this.camera_commands = null;
        this.image = new ArrayList<>();
        this.has_errors = has_errors;
    }

    /**
     * Constructor
     *
     * @param connection      the connection object
     * @param camera_commands special camera commands (direction, size, etc)
     * @param has_errors      determines if the requested image will have errors or it will be clear
     */
    public ImagePackets(Connection connection, String camera_commands, boolean has_errors) {
        this.connection = connection;
        setCamera_commands(camera_commands);
        this.image = new ArrayList<>();
        this.has_errors = has_errors;
    }


    /**
     * Sets {@link #camera_commands} and updates the values in the {@link #connection} object
     *
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
     * Sets the {@link #has_errors} value.
     *
     * @param has_errors true if the images requested will have errors false else
     */
    public void setHas_errors(boolean has_errors) {
        this.has_errors = has_errors;
    }

    /**
     * Gets the {@link #has_errors} value
     *
     * @return {@link #has_errors} current value
     */
    public boolean getHas_errors() {
        return this.has_errors;
    }

    /**
     * Adds a byte to the {@link #image} List
     *
     * @param k byte to add
     */
    public void addToImageList(byte k) {
        this.image.add(k);
    }

    /**
     * Clears the {@link #image} List from all the bytes stored
     */
    public void clearImageList() {
        this.image.clear();
    }


    /**
     * Requests one image from the server every time is called. There are two options provided by the server:
     * <ul>
     *     <li>Images with no errors</li>
     *     <li>Images with errors</li>
     * </ul>
     * <p>
     * The requested image type is determined by the {@link #has_errors} attribute. Every received image is saved to a
     * file with the function {@link #saveToFile(String file_name)}
     */
    @Override
    public void getPackets() {
        int k;  // The input buffer byte
        String request_code;  // The request code for the image

        Modem modem = this.connection.getModem();

        // Choose from between a request with errors and an error free request
        if (this.has_errors) {
            request_code = this.connection.getImage_code_error();
        } else {
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

                } catch (Exception e) {
                    // If any exception is thrown here we exit the program because something has gone wrong and there
                    // is no chance of recovering.
                    System.out.println("Exception thrown: " + e.toString());

                    System.out.println("Failed to receive image. Terminating...");
                    return;
                }

                // Add bytes to the image Byte List
                this.image.add((byte) k);
                //System.out.println(k + " ");  // debug comment

                // Detect end of image
                if (isTransmissionOver()) {
                    // Finally the image to the file
                    saveToFile(createFileName(Constants.IMAGES_DATA_DIR.getStr(), ".jpeg"));
                    this.image.clear();
                    break;
                }
            }

            System.out.println("Image received\n\n");

        } else {
            System.out.println("Unrecoverable exception occurred while receiving image. Terminating...");
        }
    }

    /**
     * Compares the last to byte elements of the {@link #image} list with 0xFF 0xD9 bytes (jpeg file ending bytes)
     *
     * @return true if both bytes are found false else
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
     * Save the image stored in the {@link #image} List as bytes to a .jpeg file.
     *
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
     *
     * @return name + date + .jpeg
     */
    @Override
    public String createFileName(String directory, String file_extension) {
        // Create the name of the image file depending on the errors
        String name = has_errors ? "Corrupted_image " : "Clear_image ";

        String pattern = "yyyy-MM-dd HH-mm-ss";

        // Create an instance of SimpleDateFormat used for formatting
        // the string representation of date according to the chosen pattern
        DateFormat df = new SimpleDateFormat(pattern);

        // Get the today date using Calendar object.
        Date today = Calendar.getInstance().getTime();

        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.

        return directory + name + df.format(today) + file_extension;
    }
}
