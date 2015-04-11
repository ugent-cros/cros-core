package drones.commands.ardrone2.atcommand;

/**
 * The command looks like: AT*CONFIG_IDS=<SEQ>,<ANIMATION_ID>,<DURATION>\r
 *
 * Created by brecht on 4/3/15.
 */
public class ATCommandANIM extends ATCommand {
    private AnimationID anim;
    private int dur;

    // The command name
    private static final String COMMAND_NAME = "ANIM";

    /**
     *
     * @param seq The sequence number of the command
     * @param anim The ID of the animation
     * @param dur The duration of the animation
     */
    public ATCommandANIM(int seq, AnimationID anim, int dur) {
        super(seq);

        this.anim = anim;
        this.dur = dur;
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,%d,%d", seq, anim.getID(), dur);
    }

    /**
     *
     * @return The name of the command
     */
    @Override
    protected String getCommandName() {
        return COMMAND_NAME;
    }

    /**
     *
     */
    public enum AnimationID {
        PHI_M30_DEG(0),
        PHI_30_DEG(1),
        THETA_M30_DEG(2),
        THETA_30_DEG(3),
        THETA_20DEG_YAW_200DEG(4),
        THETA_20DEG_YAW_M200DEG(5),
        TURNAROUND(6),
        TURNAROUND_GODOWN(7),
        YAW_SHAKE(8),
        YAW_DANCE(9),
        PHI_DANCE(10),
        THETA_DANCE(11),
        VZ_DANCE(12),
        WAVE(13),
        PHI_THETA_MIXED(14),
        DOUBLE_PHI_THETA_MIXED(15),
        FLIP_AHEAD(16),  // AR.Drone 2.0
        FLIP_BEHIND(17), // AR.Drone 2.0
        FLIP_LEFT(18),   // AR.Drone 2.0
        FLIP_RIGHT(19),  // AR.Drone 2.0
        ANIM_MAYDAY(20);

        private final int ID;

        private AnimationID(int ID) {
            this.ID = ID;
        }

        private int getID() {
            return ID;
        }
    }
}
