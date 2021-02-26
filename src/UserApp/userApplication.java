package UserApp;


import java.util.Arrays;

public class userApplication {

    /**
     * @param args program arguments:
     *             1. Echo request code   : E_XXXX
     *             2. Image request code  : M_XXXX  (Tx/Rx error free)
     *             3. Image request code  : G_XXXX  (Tx/Rx with errors)
     *             4. GPS request code    : P_XXXX
     *             5. ACK result code     : Q_XXXX
     *             6. NACK result code    : R_XXXX
     *
     */
    public static void main(String[] args){
        if (args.length != 6){
            System.out.println("Wrong arguments");
            return;
        }

        System.out.println(Arrays.toString(args));

        Tasks tasks = new Tasks(args[0], args[1], args[2], args[3], args[4], args[5]);

        tasks.testConnection();

        tasks.getEchoPackets(10);

//        for (int i = 0; i<=10; i++){
//            tasks.getImages();
//
//        }

        tasks.modem.close();
    }
}
