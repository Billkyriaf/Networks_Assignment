import ithakimodem.Modem;

public class Main {
    public static void main(String[] param) {
        (new Main()).demo();
    }

    public void demo() {
        int k;
        Modem modem = new Modem();
        modem.setSpeed(2000);  // Connection speed
        modem.setTimeout(2000);  // The timeout time. After this time with no activity the connection times out
        modem.open("ithaki");

        while (true) {
            try {
                k = modem.read();
                if (k == -1) {
                    System.out.println("Connection timed out.");
                    break;
                }

                System.out.print((char)k);

            } catch (Exception x) {
                System.out.println("Exception caught" + x.toString());
                break;
            }
        }
// NOTE : Break endless loop by catching sequence "\r\n\n\n".
// NOTE : Stop program execution when "NO CARRIER" is detected.
// NOTE : A time-out option will enhance program behavior.
// NOTE : Continue with further Java code.
// NOTE : Enjoy :)
        modem.close();
    }
}
