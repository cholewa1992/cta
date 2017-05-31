package dk.itu.jbec.sharedservices.Storage;

import java.math.BigInteger;

import dk.itu.jbec.sharedservices.Crypto.PrivateKey;

public class KeyInfo {

    private String user, service;

    public BigInteger x;
    public BigInteger y;
    public BigInteger g;
    public BigInteger p;

    public KeyInfo(String service, String user, BigInteger x, BigInteger y, BigInteger g, BigInteger p) {
        this.service = service;
        this.user = user;
        this.x = x;
        this.y = y;
        this.g = g;
        this.p = p;
    }
}
