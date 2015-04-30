package parrot.messages;

import java.io.Serializable;

/**
 * Created by Benjamin on 14/04/2015.
 */
public class AssignmentProgressChangedMessage implements Serializable {
    private int progress;

    public AssignmentProgressChangedMessage(int progress) { this.progress = progress; }

    public int getProgress() { return progress; }
}
