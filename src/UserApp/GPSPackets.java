package UserApp;

import ithakimodem.Modem;

public class GPSPackets {
    private final Connection connection;


    public GPSPackets(Connection connection) {
        this.connection = connection;

    }

    public void getGPSPackets(){
        Modem modem = connection.getModem();

        String request_code = connection.getGps_code();
        int k; // input bytes

        StringBuilder gps_line = new StringBuilder();

        // Request the image
        if (modem.write((request_code).getBytes())) {
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
                    gps_line.append((char) k);

                    // Detect end of line

                    // Detect end of gps transmission

                } catch (Exception x) {
                    System.out.println("Exception thrown: " + x.toString());
                    break;
                }
            }

            System.out.println("GPS data received: " + gps_line.toString());

        } else {
            System.out.println("Failed to send image code");
        }

    }
}
