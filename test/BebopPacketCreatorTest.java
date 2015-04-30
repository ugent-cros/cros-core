import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import java.util.TimeZone;

import controllers.*;
import parrot.ardrone3.util.PacketCreator;

/**
 * Created by Cedric on 4/6/2015.
 */
public class BebopPacketCreatorTest {

    @Test
    public void date_should_be_iso8601(){
        DateTime time = new DateTime(2015, 4, 6, 14, 15);
        String format = PacketCreator.getDateString(time);
        Assert.assertEquals(format, "2015-04-06");
    }

    @Test
    public void time_should_be_iso8601(){
        DateTime time = new DateTime(2015, 4, 6, 14, 15, DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Brussels")));
        String val = PacketCreator.getTimeString(time);
        Assert.assertEquals(val, "T141500+0200");
    }
}
