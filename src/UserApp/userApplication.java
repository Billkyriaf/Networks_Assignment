package UserApp;


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

        EchoPackets echoPackets = new EchoPackets(connection);
        ImagePackets imagePackets = new ImagePackets(connection, Commands.MOVING_CAM.getStr() + Commands.MOVE_RIGHT.getStr());

        //echoPackets.getEchoPackets(5);

        for (int i = 0; i<=20; i++) {
            imagePackets.getImage(false);
        }

        connection.getModem().close();
    }
}
