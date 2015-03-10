package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.ControllerHelper;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Benjamin on 5/03/2015.
 */
@Entity
@JsonRootName("assignment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Assignment extends Model {

    @JsonView(ControllerHelper.Summary.class)
    @Id
    public Long id;

    @Constraints.Required
    @ManyToMany(cascade = CascadeType.ALL)
    public List<Checkpoint> route;

    @Constraints.Required
    public int progress;

    @Constraints.Required
    public int priority;

    @OneToOne
    public User creator;

    @OneToOne
    public Drone assignedDrone;

    public Assignment(List<Checkpoint> route, User creator) {
        this();
        this.route = route;
        this.creator = creator;
    }

    public Assignment() {
        priority = 0;
        progress = 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(obj == this)
            return true;
        if(!(obj instanceof Assignment))
            return false;
        Assignment other = (Assignment) obj;
        boolean routesAreEqual;
        if(this.route == null || other.route == null)
            routesAreEqual = this.route == other.route;
        else
            routesAreEqual = this.route.equals(other.route);
        boolean assignedDroneIsEqual;
        if(this.assignedDrone == null || other.assignedDrone == null)
            assignedDroneIsEqual = this.assignedDrone == other.assignedDrone;
        else
            assignedDroneIsEqual = this.assignedDrone.equals(other.assignedDrone);
        boolean result = this.id.equals(other.id)
                && (other.priority == this.priority)
                && (other.progress == this.progress)
                && this.creator.equals(other.creator)
                && assignedDroneIsEqual
                && routesAreEqual;
        System.out.println(result);
        return result;
    }

    public static Finder<Long,Assignment> find = new Finder<>(Long.class, Assignment.class);

}
