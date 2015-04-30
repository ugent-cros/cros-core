package droneapi.messages;

import droneapi.model.properties.PropertyType;

import java.io.Serializable;

/**
 * Created by Cedric on 3/10/2015.
 */
public class PropertyRequestMessage implements Serializable {
    private PropertyType type;

    public PropertyRequestMessage(PropertyType type) {
        this.type = type;
    }

    public PropertyType getType() {
        return type;
    }
}
