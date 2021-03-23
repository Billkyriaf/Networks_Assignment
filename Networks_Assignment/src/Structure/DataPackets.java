package Structure;

/**
 * This interface dictates the methods needed for all the types of connections.
 * For easier data manipulation all packets will be saved to the respected file.
 */
public interface DataPackets {

    /**
     * Starts getting the packets from the server and handles the connection
     */
    void getPackets();

    /**
     * Checks if the transmission is over so the connection does not timeout
     * @return true if the transmission is over false else
     */
    boolean isTransmissionOver();

    /**
     * Saves data to file for later usage
     * @param file_name the name of the file
     */
    void saveToFile(String file_name);

    /**
     * Returns a unique file name based on the current dateTime
     * @param directory the directory to save the file
     * @param file_extension the file extension
     * @return name + dateTime
     */
    String createFileName(String directory, String file_extension);
}
