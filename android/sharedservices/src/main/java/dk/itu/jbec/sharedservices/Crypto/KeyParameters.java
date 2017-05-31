package dk.itu.jbec.sharedservices.Crypto;

import java.io.Serializable;
import java.math.BigInteger;

public class KeyParameters implements Serializable {

    private final BigInteger p, g;

    public KeyParameters(BigInteger p, BigInteger g) {
        this.p = p;
        this.g = g;
    }

    public BigInteger getP() {
        return p;
    }
    public BigInteger getG() {
        return g;
    }

}
