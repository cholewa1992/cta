package cta.dtos;

import java.io.Serializable;

/**
 * Created by wismann on 01/05/2017.
 */
public class UserPubKey implements Serializable {
    private String pubKeyB64;

    private String username;


    protected UserPubKey() {};

    public UserPubKey(String pubKeyB64, String username) {
        this.pubKeyB64 = pubKeyB64;
        this.username = username;
    }

    public String getPubKeyB64() {
        return pubKeyB64;
    }

    public void setPubKeyB64(String pubKeyB64) {
        this.pubKeyB64 = pubKeyB64;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
