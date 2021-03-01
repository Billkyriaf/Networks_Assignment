package Stracture;

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

        String[] parsed_message = this.line.split(",|\\*");

        if (parsed_message.length == 14) {
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
            this.mode = parsed_message[12];
            this.checksum = parsed_message[13];

        } else {
            System.out.println("Error parsing data too many or too few arguments!");
        }
    }

    public String getLine() {
        return this.line;
    }

    public String getCoordinates(){

        String latitude_deg = this.latitude.substring(0, 2);
        String latitude_min = this.latitude.substring(2, 4);
        String latitude_sec = "0" + this.latitude.substring(4);

        double lat_sec = Double.parseDouble(latitude_sec);

        lat_sec = lat_sec * 60;

        latitude_sec = String.valueOf((int)Math.round(lat_sec));

        String latitude = latitude_deg + latitude_min + latitude_sec;


        String longitude_deg = this.longitude.substring(1, 3);
        String longitude_min = this.longitude.substring(3, 5);
        String longitude_sec = "0" + this.longitude.substring(5);

        double long_sec = Double.parseDouble(longitude_sec);

        long_sec = long_sec * 60;

        longitude_sec = String.valueOf((int)Math.round(long_sec));

        String longitude = longitude_deg + longitude_min + longitude_sec;

        return longitude + latitude;
    }
}
