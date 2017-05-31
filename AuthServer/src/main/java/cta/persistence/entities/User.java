package cta.persistence.entities;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by wismann on 19/04/2017.
 */
@Entity
@Table(name = "User")
public class User implements Serializable {

    @Id
    @Column(name = "username")
    private String username;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "key_id")
    private PubKey pubKey;

    // jpa required constructor
    protected User() {
    }

    public User(String username, PubKey pubKey) {
        this.username = username;
        this.pubKey = pubKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PubKey getPubKey() {
        return pubKey;
    }

    public void setPubKey(PubKey pubKey) {
        this.pubKey = pubKey;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", pubKey=" + pubKey +
                '}';
    }
}
