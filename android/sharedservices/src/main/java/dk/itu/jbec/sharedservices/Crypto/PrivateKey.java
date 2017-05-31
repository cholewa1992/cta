package dk.itu.jbec.sharedservices.Crypto;

import java.io.Serializable;
import java.math.BigInteger;

public class PrivateKey extends PublicKey implements Serializable {

    private transient final BigInteger x;

    public PrivateKey(BigInteger x, BigInteger y, BigInteger p, BigInteger g) {
        super(y, p, g);
        this.x = x;
    }

    public BigInteger getX() {
        return x;
    }

    public PublicKey getPublicKey(){
        return new PublicKey(getY(), getP(), getG());
    }

}
