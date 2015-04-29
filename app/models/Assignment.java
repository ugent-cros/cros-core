package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.JsonHelper;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Benjamin on 5/03/2015.
 */
@Entity
@JsonRootName("assignment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Assignment extends Model {

    public static final Finder<Long,Assignment> FIND = new Finder<>(Long.class, Assignment.class);

    @JsonView(JsonHelper.Summary.class)
    @Id
    private Long id;

    @Version
    @JsonIgnore
    public Long version;

    @Constraints.Required
    @OneToMany(cascade = CascadeType.ALL)
    @OrderColumn
    private List<Checkpoint> route;

    @Constraints.Required
    private boolean scheduled;

    @Constraints.Required
    private int progress;

    @JsonView(JsonHelper.Summary.class)
    @Constraints.Required
    private int priority;

    @JsonView(JsonHelper.Summary.class)
    @OneToOne
    private User creator;

    @JsonView(JsonHelper.Summary.class)
    @OneToOne(optional = true)
    private Drone assignedDrone;

    public Assignment(List<Checkpoint> route, User creator) {
        this();
        this.route = route;
        this.creator = creator;
    }

    public Assignment() {
        priority = 0;
        progress = 0;
        scheduled = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Checkpoint> getRoute() { return route; }

    public void setRoute(List<Checkpoint> route) {
        this.route = route;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isScheduled(){
        return scheduled;
    }

    public void setScheduled(boolean scheduled){
        this.scheduled = scheduled;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Drone getAssignedDrone() {
        return assignedDrone;
    }

    public void setAssignedDrone(Drone assignedDrone) {
        this.assignedDrone = assignedDrone;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj == null || !(obj instanceof Assignment))
            return false;
        Assignment other = (Assignment) obj;
        boolean isEqual = this.id.equals(other.id);
        isEqual &= (other.priority == this.priority);
        isEqual &= (other.progress == this.progress);
        isEqual &= this.creator.equals(other.creator);
        isEqual &= assignedDroneIsEqual(other);
        return isEqual && routesAreEqual(other);
    }

    private boolean routesAreEqual(Assignment other) {
        if(this.route == null || other.route == null)
            return this.route == other.route;
        else
            return this.route.equals(other.route);
    }

    private boolean assignedDroneIsEqual(Assignment other) {
        if(this.assignedDrone == null || other.assignedDrone == null)
            return this.assignedDrone == other.assignedDrone;
        else
            return this.assignedDrone.equals(other.assignedDrone);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (route != null ? route.hashCode() : 0);
        result = 31 * result + progress;
        result = 31 * result + priority;
        result = 31 * result + (creator != null ? creator.hashCode() : 0);
        result = 31 * result + (assignedDrone != null ? assignedDrone.hashCode() : 0);
        return result;
    }
}
