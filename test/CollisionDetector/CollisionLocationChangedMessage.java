package CollisionDetector;

import droneapi.api.DroneCommander;
import droneapi.messages.LocationChangedMessage;
import droneapi.model.properties.Location;

import java.io.Serializable;

/**
 * Same as LocationChangedMessage but with dronecommander
 *
 * Created by Sander on 30/04/2015.
 */
public class CollisionLocationChangedMessage implements Serializable{

    private LocationChangedMessage locationChangedMessage;

    private Long droneId;

    public CollisionLocationChangedMessage(LocationChangedMessage locationChangedMessage, Long droneId) {
        this.locationChangedMessage = locationChangedMessage;
        this.droneId = droneId;
    }

    public LocationChangedMessage getLocationChangedMessage() {
        return locationChangedMessage;
    }

    public Long getDroneId() {
        return droneId;
    }

    public Location getLocation(){
        return new Location(locationChangedMessage.getLatitude(),locationChangedMessage.getLongitude(),locationChangedMessage.getGpsHeight());
    }
}
