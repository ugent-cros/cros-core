package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by Eveline on 6/03/2015.
 */

@Entity
@JsonRootName("checkpoint")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Checkpoint extends Model {

    @Id
    public Long id;

    @Constraints.Required
    public double longitude;

    @Constraints.Required
    public double lattitude;

    @Constraints.Required
    public double altitude;

    @Constraints.Required
    public int waitingTime;

    public Checkpoint (double longitude, double lattitude, double altitude){
        this.longitude = longitude;
        this.lattitude = lattitude;
        this.altitude = altitude;
        this.waitingTime = 0;
    }

    public Checkpoint (double longitude, double lattitude, double altitude, int waitingTime){
        this.longitude = longitude;
        this.lattitude = lattitude;
        this.altitude = altitude;
        this.waitingTime = waitingTime;
    }

    public Checkpoint() {
        this(0,0,0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Checkpoint))
            return false;
        Checkpoint checkpoint = (Checkpoint) obj;
        return this.id.equals(checkpoint.id)
                && this.longitude == checkpoint.longitude
                && this.lattitude == checkpoint.lattitude
                && this.altitude == checkpoint.altitude
                && this.waitingTime == checkpoint.waitingTime;
    }

    public static Finder<Long, Checkpoint> find = new Finder<Long, Checkpoint>(Long.class, Checkpoint.class);
}
