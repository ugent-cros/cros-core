package droneapi.messages;

import java.io.Serializable;

/**
 * Created by brecht on 4/20/15.
 */
public class ImageMessage implements Serializable {

    private byte[] data;

    /**
     *
     * @param data The image data as a byte array
     */
    public ImageMessage(byte[] data) {
        this.data = data;
    }

    /**
     *
     * @return The image data as a base 64 string
     */
    public byte[] getByteData() {
        return data;
    }
}
