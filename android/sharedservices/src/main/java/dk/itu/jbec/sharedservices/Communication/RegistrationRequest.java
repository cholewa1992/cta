package dk.itu.jbec.sharedservices.Communication;

import java.io.Serializable;

import dk.itu.jbec.sharedservices.Crypto.KeyParameters;

public class RegistrationRequest implements Serializable {

    public final String serviceIdentifier;
    public final String userId;
    public final KeyParameters params;

    public RegistrationRequest(String serviceIdentifier, String userId, KeyParameters params) {
        this.serviceIdentifier = serviceIdentifier;
        this.userId = userId;
        this.params = params;
    }

}
