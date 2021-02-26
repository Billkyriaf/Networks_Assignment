package UserApp;

public enum Commands {
    // AT UserApp.Commands
    AT("AT\r"),
    ATI("ATI\r"),
    ATS("ATS\r"),
    ATSpeed("ATS="),
    ATH1("ATH1\r"),
    ATH0("ATH0\r"),
    HANG_UP("+++ATH0\r"),

    // Modes
    DATA_MODE("ITHAKI"),
    COPTER_MODE("ITHAKICOPTER");

    private final String command;

    Commands(String command){
        this.command = command;
    }

    public String getStr(){
        return this.command;
    }
}
