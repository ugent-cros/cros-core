package drones.messages;

import java.io.Serializable;
import java.util.Base64;

/**
 * Created by brecht on 4/17/15.
 */
public class ImageChangedMessage implements Serializable {

    private String image;

    public ImageChangedMessage(String image) {
        this.image = image;
    }

    /**
     *
     * @return The image in base 64 encoding
     */
    public String getImage() {
        return image;
    }
}
