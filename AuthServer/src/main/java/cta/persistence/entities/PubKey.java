package cta.persistence.entities;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by wismann on 19/04/2017.
 */
@Entity
@Table(name = "PubKey")
public class PubKey implements Serializable {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @Column(name = "y_val")
    private String y;

    @Column(name = "g_val")
    private String g;

    @Column(name = "p_val")
    private String p;

    // jpa required constructor
    protected PubKey() {
    }

    public PubKey(String y, String g, String p) {
        this.y = y;
        this.g = g;
        this.p = p;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }

    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

    @Override
    public String toString() {
        return "PubKey{" +
                "id=" + id +
                ", y='" + y + '\'' +
                ", g='" + g + '\'' +
                ", p='" + p + '\'' +
                '}';
    }
}
