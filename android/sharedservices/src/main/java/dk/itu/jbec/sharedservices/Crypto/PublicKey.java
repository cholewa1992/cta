package dk.itu.jbec.sharedservices.Crypto;

import java.io.Serializable;
import java.math.BigInteger;

public class PublicKey extends KeyParameters implements Serializable {

    private final BigInteger y;

    public PublicKey(BigInteger y, BigInteger p, BigInteger g) {
        super(p, g);
        this.y = y;
    }

    public BigInteger getY() {
        return y;
    }

    public KeyParameters getParams() { return new KeyParameters(getP(), getG()); };

}
