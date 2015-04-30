package CollisionDetector;

import api.DroneCommander;
import messages.LocationChangedMessage;
import model.properties.Location;

import java.io.Serializable;

/**
 * Same as LocationChangedMessage but with dronecommander
 *
 * Created by Sander on 30/04/2015.
 */
public class CollisionLocationChangedMessage implements Serializable{

    private LocationChangedMessage locationChangedMessage;

    private DroneCommander droneCommander;

    public CollisionLocationChangedMessage(LocationChangedMessage locationChangedMessage, DroneCommander droneCommander) {
        this.locationChangedMessage = locationChangedMessage;
        this.droneCommander = droneCommander;
    }

    public LocationChangedMessage getLocationChangedMessage() {
        return locationChangedMessage;
    }

    public DroneCommander getDroneCommander() {
        return droneCommander;
    }

    public Location getLocation(){
        return new Location(locationChangedMessage.getLatitude(),locationChangedMessage.getLongitude(),locationChangedMessage.getGpsHeight());
    }
}
