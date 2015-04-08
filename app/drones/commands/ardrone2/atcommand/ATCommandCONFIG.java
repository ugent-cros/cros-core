package drones.commands.ardrone2.atcommand;

import drones.protocols.ardrone2.ConfigKeys;

/**
 * The command looks like: AT*CONFIG=<SEQ>,"<KEY>","<VALUE>"\r
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandCONFIG extends ATCommand {
    // The key of the config command
    private ConfigKeys key;
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
    public ATCommandCONFIG(int seq, ConfigKeys key, String value) {
        super(seq);
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

    /**
     *
     * @return The name of the command
     */
    @Override
    protected String getCommandName() {
        return COMMAND_NAME;
    }

}
