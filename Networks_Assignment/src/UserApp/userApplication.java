package UserApp;


import Echo.EchoErrors;
import Echo.EchoPackets;
import GPS.GPSPackets;
import Image.ImagePackets;
import Structure.Connection;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Arrays;

public class userApplication {

    /**
     * Runs all the tests required. Expects 6 command line arguments as seen below.
     *
     * @param args program arguments:
     *             1. Echo request code   : E_XXXX
     *             2. Image request code  : M_XXXX  (Tx/Rx error free)
     *             3. Image request code  : G_XXXX  (Tx/Rx with errors)
     *             4. GPS request code    : P_XXXX
     *             5. ACK result code     : Q_XXXX
     *             6. NACK result code    : R_XXXX
     */
    public static void main(String[] args) {
        // Check if the arguments are correct
        if (args.length != 6) {
            System.out.println("Expected: <EXXXX> <MXXXX> <GXXXX> <PXXXX> <QXXXX> <RXXXX> as arguments");
            return;
        }

        /* Create the connection with the server
         * IMPORTANT NOTE!!  for the GPS module a 4 second delay is used at some points if the timeout parameter is
         * less that 5 the connection will time out.
         */
        Connection connection = new Connection(args[0], args[1], args[2], args[3], args[4], args[5], 80000, 10000);


        // Comment or uncomment one or more of the following sections to run the tests you like!!

        // ===================================   Echo packets   ===================================
        EchoPackets echoPackets = new EchoPackets(connection, 5000);
        connection.setModemSpeed(3800);

        // Get 5 minutes of echo packets
        echoPackets.getPackets();



        // ===================================   Echo packets with errors   ===================================
        EchoErrors echoErrors = new EchoErrors(connection, 5000);
        connection.setModemSpeed(3800);

        // Get 5 minutes of echo packets with errors
        echoErrors.getPackets();



        // ===================================   Image   ===================================
        ImagePackets clear_image = new ImagePackets(connection, false);
        connection.setModemSpeed(80000);

        // Get single image
        clear_image.getPackets();

        // Gets 10 images with the camera rotating left
        clear_image.setCamera_commands("CAM=PTZ DIR=L");
        for (int i = 0; i < 10; i++)
            clear_image.getPackets();



        // ===================================   Corrupted image   ===================================
        ImagePackets corrupted_image = new ImagePackets(connection, true);
        connection.setModemSpeed(80000);

        // Get a corrupted image
        corrupted_image.getPackets();



        // ===================================   GPS data   ===================================
        GPSPackets gpsData = new GPSPackets(connection, "R=1000199");
        connection.setModemSpeed(80000);

        // Get the gps data and the gps image
        gpsData.getPackets();



        // ===================================    Get the hole route 1   ===================================
        ArrayList<String> codes = new ArrayList<>(Arrays.asList("R=1000199", "R=1010099", "R=1020099", "R=1030099",
                "R=1040099", "R=1050099", "R=1060099", "R=1070099", "R=1080099", "R=1090099", "R=1100099"));

        for (String code: codes) {
            // GPS packets
            GPSPackets gpsRoute = new GPSPackets(connection, code);
            gpsRoute.getPackets();
        }




        // Finally close the connection with the server
        try {
            connection.getModem().close();
        }
        catch (Exception e){
            System.out.println("Exception caught while trying to close the modem connection");
        }

    }
}
