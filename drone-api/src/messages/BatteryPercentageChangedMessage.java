package messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class BatteryPercentageChangedMessage implements Serializable {
    private byte percent;

    public BatteryPercentageChangedMessage(byte percent) {
        this.percent = percent;
    }

    public byte getPercent() {
        return percent;
    }
}
