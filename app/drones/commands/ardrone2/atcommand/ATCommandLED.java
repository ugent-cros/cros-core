package drones.commands.ardrone2.atcommand;

/**
 * The command looks like: AT*CONFIG_IDS=<SEQ>,<ANIMATION_ID>,<FREQUENCY>,<DURATION>\r
 *
 * Created by brecht on 4/3/15.
 */
public class ATCommandLED extends ATCommand {
    private LEDID anim;
    private float freq;
    private int dur;

    // The command name
    private static final String COMMAND_NAME = "LED";

    /**
     *
     * @param seq The sequence number of the command
     * @param anim The ID of the animation
     * @param freq The frequency of the animation [Hz]
     * @param dur The duration of the animation [s]
     */
    public ATCommandLED(int seq, LEDID anim, float freq, int dur) {
        super(seq, COMMAND_NAME);
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,%d,%d,%d", seq, anim.getID(), intOfFloat(freq), dur);
    }


    /**
     *
     */
    public enum LEDID {
        BLINK_GREEN_RED(0),
        BLINK_GREEN(1),
        BLINK_RED(2),
        BLINK_ORANGE(3),
        SNAKE_GREEN_RED(4),
        FIRE(5),
        STANDARD(6),
        RED(7),
        GREEN(8),
        RED_SNAKE(9),
        BLANK(10),
        RIGHT_MISSILE(11),
        LEFT_MISSILE(12),
        DOUBLE_MISSILE(13),
        FRONT_LEFT_GREEN_OTHERS_RED(14),
        FRONT_RIGHT_GREEN_OTHERS_RED(15),
        REAR_RIGHT_GREEN_OTHERS_RED(16),
        REAR_LEFT_GREEN_OTHERS_RED(17),
        LEFT_GREEN_RIGHT_RED(18),
        LEFT_RED_RIGHT_GREEN(19),
        BLINK_STANDARD(20),
        ARDRONE_NB_LED_ANIM_MAYDAY(21);

        private final int id;

        private LEDID(int id) {
            this.id = id;
        }

        private int getID() {
            return id;
        }
    }
}
