package models;

import com.fasterxml.jackson.annotation.JsonRootName;
import controllers.routes;
import play.data.validation.Constraints;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Benjamin on 5/03/2015.
 */
@Entity
@JsonRootName("assignment")
public class Assignment extends Resource {

    @Id
    public Long id;

    //TODO: list has to contain checkpoint objects
    @Constraints.Required
    @ElementCollection
    public List<String> route;

    @Constraints.Required
    public int progress;

    @Constraints.Required
    public int priority;

    @Constraints.Required
    @OneToOne
    public User creator;

    @Constraints.Required
    @OneToOne
    public Drone assignedDrone;

    public Assignment(List<String> route, User creator) {
        this.route = route;
        this.creator = creator;
        priority = 0;
        progress = 0;
    }

    public static Finder<Long,Assignment> find = new Finder<>(Long.class, Assignment.class);

    @Override
    public List<Link> getLinksList() {
        List<Link> links = new ArrayList<>();
        links.add(new Link("all", routes.AssignmentController.getAllAssignments().url()));
        links.add(new Link("create", routes.AssignmentController.createAssignment().url()));
        links.add(new Link("get", routes.AssignmentController.getAssignment(id).url()));
        links.add(new Link("delete", routes.AssignmentController.deleteAssigment(id).url()));
        return links;
    }
}
