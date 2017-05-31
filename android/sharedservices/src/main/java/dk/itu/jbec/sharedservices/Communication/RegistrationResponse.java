package dk.itu.jbec.sharedservices.Communication;

import java.io.Serializable;

import dk.itu.jbec.sharedservices.Crypto.PublicKey;

public class RegistrationResponse implements Serializable {

    public final PublicKey key;

    public RegistrationResponse(PublicKey key) {
        this.key = key;
    }
}
