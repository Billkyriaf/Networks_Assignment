package UserApp;


import Echo.EchoPackets;
import GPS.GPSPackets;
import Image.ImagePackets;
import Stracture.Connection;

public class userApplication {

    /**
     * Runs all the tests required.
     *
     * @param args program arguments:
     *             1. Echo request code   : E_XXXX
     *             2. Image request code  : M_XXXX  (Tx/Rx error free)
     *             3. Image request code  : G_XXXX  (Tx/Rx with errors)
     *             4. GPS request code    : P_XXXX
     *             5. ACK result code     : Q_XXXX
     *             6. NACK result code    : R_XXXX
     */
    public static void main(String[] args){
        // Check if the arguments are correct
        if (args.length != 6){
            System.out.println("Expected: <EXXXX> <MXXXX> <GXXXX> <PXXXX> <QXXXX> <RXXXX> as arguments");
            return;
        }

        Connection connection = new Connection(args[0], args[1], args[2], args[3], args[4], args[5], 76000, 10000);

        // Echo packets
        EchoPackets echoPackets = new EchoPackets(connection, 5);

        // Image packets
        ImagePackets clear_image = new ImagePackets(connection, false);

        ImagePackets corrupted_image = new ImagePackets(connection, true);

        // GPS packets
        GPSPackets gpsData = new GPSPackets(connection, new ImagePackets(connection, false));



        // Run some tests
        echoPackets.getPackets();

        //clear_image.getPackets();

        //corrupted_image.getPackets();

        //gpsData.getPackets();

        connection.getModem().close();
    }
}
