package Stracture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class gpsGPGSA {
    final String message_ID = "$GPGSA";
    String mode_1;
    String mode_2;
    List<String> satellites;
    String pdop;
    String hdop;
    String vdop;
    String checksum;

    String line;

    public gpsGPGSA(String line){
        this.line = line;
        this.satellites = new ArrayList<>();
        String[] parsed_message = this.line.split(",|\\*");

        if (parsed_message.length == 19) {
            this.mode_1 = parsed_message[1];
            this.mode_2 = parsed_message[2];
            this.satellites.addAll(Arrays.asList(parsed_message).subList(3, 15));
            this.pdop = parsed_message[15];
            this.hdop = parsed_message[16];
            this.vdop = parsed_message[17];
            this.checksum = parsed_message[18];
        }
        else {
            System.out.println("Error parsing data too many or too few arguments!");
        }
    }

    public String getLine() {
        return this.line;
    }
}
