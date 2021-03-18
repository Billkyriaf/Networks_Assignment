"""
This file holds constant values used in the program
"""


# End of Sequences
SM_END = "\r\n\n\n"  # Server message end
PACKET_END = "PSTOP"  # Echo packet end
GPS_DATA_LINE_END = "\r\n"
GPS_TRANSMISSION_END = "STOP ITHAKI GPS TRACKING\r\n"
GPS_TRANSMISSION_START = "START ITHAKI GPS TRACKING\r\n"
GPGGA = "$GPGGA"
GPGSA = "$GPGSA"
GPRMC = "$GPRMC"

# Data input directories
GPS_DATA_DIR = "./../../Networks_Assignment/GPS_Saved_Data/"
GPS_IMAGES_DIR = "./../../Networks_Assignment/GPS_Saved_Data/GPS Images/"
ECHO_DATA_DIR = "./../../Networks_Assignment/Echo_Saved_Data/Normal Packets/"
ERR_ECHO_DATA_DIR = "./../../Networks_Assignment/Echo_Saved_Data/Error Packets/"
IMAGES_DATA_DIR = "./../../Networks_Assignment/Images_Saved_Data/"

# File names
ECHO_FILE_NAME = "echo_packets *"
ERR_ECHO_FILE_NAME = "err_echo_packets *"
