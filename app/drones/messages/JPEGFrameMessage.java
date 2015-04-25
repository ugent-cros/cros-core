package drones.messages;

import java.io.Serializable;

/**
 * Created by brecht on 4/20/15.
 */
public class JPEGFrameMessage implements Serializable {

    private String imageData;

    /**
     *
     * @param imageData The image data as a base 64 string
     */
    public JPEGFrameMessage(String imageData) {
        this.imageData = imageData;
    }

    /**
     *
     * @return The image data as a base 64 string
     */
    public String getImageData() {
        return imageData;
    }
}
