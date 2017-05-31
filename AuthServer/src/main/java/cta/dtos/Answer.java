package cta.dtos;

import java.io.Serializable;

/**
 * Created by wismann on 25/04/2017.
 */
public class Answer implements Serializable {

    private String username;
    private String decChal;

    public Answer(String username, String decChal) {
        this.username = username;
        this.decChal = decChal;
    }

    protected Answer() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDecChal() {
        return decChal;
    }

    public void setDecChal(String decChal) {
        this.decChal = decChal;
    }

    @Override
    public String toString() {
        return "Answer{" +
                "username='" + username + '\'' +
                ", decChal='" + decChal + '\'' +
                '}';
    }
}
