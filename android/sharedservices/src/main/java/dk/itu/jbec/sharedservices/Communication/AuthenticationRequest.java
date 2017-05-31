package dk.itu.jbec.sharedservices.Communication;

import java.io.Serializable;

public class AuthenticationRequest implements Serializable {

    public final String serviceIdentifier;
    public final String userId;
    public final byte[] challenge;

    public AuthenticationRequest(String serviceIdentifier, String userId, byte[] challenge) {
        this.serviceIdentifier = serviceIdentifier;
        this.userId = userId;
        this.challenge = challenge;
    }
}
