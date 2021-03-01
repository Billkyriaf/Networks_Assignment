package Stracture;


import java.util.Arrays;

public class gpsGPGGA {
    final String message_ID = "$GPGGA";
    String utcTime;
    String latitude;
    String NS_indicator;
    String longitude;
    String EW_indicator;
    String position_fix_indicator;
    String satellites_used;
    String hdop;
    String MSL_altitude;
    String altitude_units;
    String geoid_separation;
    String separation_units;
    String age_of_diff_correction;
    String diff_ref_station_ID = "0000";
    String checksum;

    String line;

    public gpsGPGGA(String line){
        this.line = line;

        String[] parsed_message = this.line.split(",|\\*");

//        System.out.println(Arrays.toString(parsed_message));

        if (parsed_message.length == 16) {
            this.utcTime = parsed_message[1];
            this.latitude = parsed_message[2];
            this.NS_indicator = parsed_message[3];
            this.longitude = parsed_message[4];
            this.EW_indicator = parsed_message[5];
            this.position_fix_indicator = parsed_message[6];
            this.satellites_used = parsed_message[7];
            this.hdop = parsed_message[8];
            this.MSL_altitude = parsed_message[9];
            this.altitude_units = parsed_message[10];
            this.geoid_separation = parsed_message[11];
            this.separation_units = parsed_message[12];
            this.age_of_diff_correction = parsed_message[13];
            this.diff_ref_station_ID = parsed_message[14];
            this.checksum = parsed_message[15];
        } else {
            System.out.println("Error parsing data too many or too few arguments!");
        }
    }

    public String getLine() {
        return this.line;
    }
}

