package drones.commands.ArDrone2.ATCommand;

/**
 * Created by brecht on 3/8/15.
 */
public class ATCommandCONFIG extends ATCommand {
    private static final String TYPE = "AT*CONFIG";

    private String key;
    private String value;

    public ATCommandCONFIG(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%s=%d,\"%s\",\"%s\"\r",TYPE, seq, key, value);
    }
}
