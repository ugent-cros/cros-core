package drones.ardrone2.commands;

import drones.ardrone2.util.ConfigKey;

/**
 * The command looks like: AT*CONFIG=<SEQ>,"<KEY>","<VALUE>"\r
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandCONFIG extends ATCommand {
    // The key of the config command
    private ConfigKey key;
    // The value of the config command
    private String value;

    // The command name
    private static final String COMMAND_NAME = "CONFIG";

    /**
     *
     * @param seq The sequence number of the command
     * @param key The key of the config command
     * @param value The value of the config command
     */
    public ATCommandCONFIG(int seq, ConfigKey key, String value) {
        super(seq, COMMAND_NAME);
        this.key = key;
        this.value = value;
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,\"%s\",\"%s\"", seq, key.getKey(), value);
    }


}
