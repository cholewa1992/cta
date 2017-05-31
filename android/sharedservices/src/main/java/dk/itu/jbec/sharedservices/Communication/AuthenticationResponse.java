package dk.itu.jbec.sharedservices.Communication;

import java.io.Serializable;

public class AuthenticationResponse implements Serializable {

    public final byte[] cipher;

    public AuthenticationResponse(byte[] cipher) {
        this.cipher = cipher;
    }
}
