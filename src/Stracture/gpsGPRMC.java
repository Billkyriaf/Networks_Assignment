package Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class gpsGPRMC {
    final String message_ID = "$GPRMC";
    String utcTime;
    String status;
    String latitude;
    String NS_indicator;
    String longitude;
    String EW_indicator;
    String speed_over_ground;
    String course_over_ground;
    String date;
    String magnetic_variation;
    String mode;
    String checksum;

    String line;


    public gpsGPRMC(String line){
        this.line = line;

        String[] parsed_message = this.line.split(",");

        if (parsed_message.length == 13) {
            this.utcTime = parsed_message[1];
            this.status = parsed_message[2];
            this.latitude = parsed_message[3];
            this.NS_indicator = parsed_message[4];
            this.longitude = parsed_message[5];
            this.EW_indicator = parsed_message[6];
            this.speed_over_ground = parsed_message[7];
            this.course_over_ground = parsed_message[8];
            this.date = parsed_message[9];
            this.magnetic_variation = parsed_message[10];
            this.mode = parsed_message[11];
            this.checksum = parsed_message[12];

        } else {
            System.out.println("Error parsing data too many or too few arguments!");
        }
    }
}
