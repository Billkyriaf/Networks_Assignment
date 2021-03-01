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

    public String getCoordinates(){
        String longitude_deg = this.longitude.substring(1, 3);
        String longitude_min = this.longitude.substring(3, 5);
        String longitude_sec = "0" + this.longitude.substring(5);

        double long_sec = Double.parseDouble(longitude_sec);

        long_sec = long_sec * 60;

        longitude_sec = String.valueOf((int)Math.round(long_sec));

        String longitude = longitude_deg + longitude_min + longitude_sec;


        String latitude_deg = this.latitude.substring(0, 2);
        String latitude_min = this.latitude.substring(2, 4);
        String latitude_sec = "0" + this.latitude.substring(4);

        double lat_sec = Double.parseDouble(latitude_sec);

        lat_sec = lat_sec * 60;

        latitude_sec = String.valueOf((int)Math.round(lat_sec));

        String latitude = latitude_deg + latitude_min + latitude_sec;


        return longitude + latitude;
    }
}

