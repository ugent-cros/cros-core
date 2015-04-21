package drones.messages;

import java.io.Serializable;

/**
 * Created by brecht on 4/20/15.
 */
public class JPEGFrameMessage implements Serializable {

    private String imageData;

    public JPEGFrameMessage(String imageData) {
        this.imageData = imageData;
    }

    public String getImageData() {
        return imageData;
    }
}
