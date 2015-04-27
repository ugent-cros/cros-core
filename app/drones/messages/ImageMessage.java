package drones.messages;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
