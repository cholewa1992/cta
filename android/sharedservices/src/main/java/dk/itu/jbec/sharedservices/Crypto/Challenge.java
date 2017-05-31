package dk.itu.jbec.sharedservices.Crypto;

import java.io.Serializable;
import java.math.BigInteger;

public class Challenge {

    private final byte[] nonce;
    private final byte[] cipher;

    public Challenge(byte[] nonce, byte[] cipher){

        this.nonce = nonce;
        this.cipher = cipher;
    }

    public byte[] getCipher() {
        return cipher;
    }

    public boolean check(byte[] response) {
        return new BigInteger(1, nonce).equals(new BigInteger(1, response));
    }

}
