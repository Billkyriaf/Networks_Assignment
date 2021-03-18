package Structure;

public enum Constants {
    // AT Structure.Commands
    AT("AT\r"),
    ATI("ATI\r"),
    ATS("ATS\r"),
    ATSpeed("ATS="),
    ATH1("ATH1\r"),
    ATH0("ATH0\r"),
    HANG_UP("+++ATH0\r"),

    // Modes
    DATA_MODE("ITHAKI"),
    COPTER_MODE("ITHAKICOPTER"),

    //End of Sequences
    SM_END("\r\n\n\n"),
    PACKET_END("PSTOP"),
    AT_END("\r\n"),
    GPS_DATA_LINE_END("\r\n"),
    GPS_TRANSMISSION_END("STOP ITHAKI GPS TRACKING\r\n"),
    GPS_TRANSMISSION_START("START ITHAKI GPS TRACKING\r\n"),
    GPGGA("$GPGGA"),
    GPGSA("$GPGSA"),
    GPRMC("$GPRMC"),

    //Camera Controls
    STEADY_CAM("CAM=FIX"),
    MOVING_CAM("CAM=PTZ"),
    MOVE_LEFT("DIR=L"),
    MOVE_RIGHT("DIR=R"),
    MOVE_UP("DIR=U"),
    MOVE_DOWN("DIR=D"),
    MEMORISE_DIR("M"),
    RECALL_MEMORY_DIR("C"),

    // Data output directories
    GPS_DATA_DIR("GPS_Saved_Data/"),
    GPS_IMAGES_DIR("GPS_Saved_Data/GPS Images/"),
    ECHO_DATA_DIR("Echo_Saved_Data/Normal Packets/"),
    ERR_ECHO_DATA_DIR("Echo_Saved_Data/Error Packets/"),
    IMAGES_DATA_DIR("Images_Saved_Data/"),

    // File names
    ECHO_FILE_NAME("echo_packets "),
    ERR_ECHO_FILE_NAME("err_echo_packets ");


    private final String constant;

    Constants(String constant){
        this.constant = constant;
    }

    public String getStr(){
        return this.constant;
    }
}
