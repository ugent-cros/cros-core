package models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;

/**
 * Created by Eveline on 6/03/2015.
 */
@Entity
@Table(name="basestation")
public class Basestation extends Model {

    @Id
    public Long id;

    @Constraints.Required
    @Column(length = 256, unique = true, nullable = false)
    public String name;

    @OneToOne(cascade = CascadeType.REMOVE)
    public Checkpoint checkpoint;

    public static Finder<Long, Basestation> find = new Finder<Long, Basestation>(Long.class, Basestation.class);

    public Basestation(String name, Checkpoint checkpoint){
        this.name = name;
        this.checkpoint = checkpoint;
    }
}
