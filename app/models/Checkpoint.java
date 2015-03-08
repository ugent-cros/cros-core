package models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;

/**
 * Created by Eveline on 6/03/2015.
 */

@Entity
@Table(name="checkpoint")
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
}
