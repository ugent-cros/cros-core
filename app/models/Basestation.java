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

    @Constraints.Required
    public Checkpoint checkpoint;


    public Basestation(String name, Checkpoint checkpoint){
        this.name = name;
        this.checkpoint = checkpoint;
    }
}
