package drones.models.ATCommand;

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
        String params = "\"" + key + "\"" + "," + "\"" + value + "\"";
        return (TYPE + "=" + seq + "," + params + "\r");
    }
}
