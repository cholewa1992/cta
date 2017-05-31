package dk.itu.jbec.sharedservices.Crypto;

import android.util.Pair;

import org.jetbrains.annotations.TestOnly;
import org.spongycastle.jce.interfaces.ElGamalPrivateKey;
import org.spongycastle.jce.interfaces.ElGamalPublicKey;
import org.spongycastle.jce.spec.ElGamalParameterSpec;
import org.spongycastle.jce.spec.ElGamalPrivateKeySpec;
import org.spongycastle.jce.spec.ElGamalPublicKeySpec;
import org.spongycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class DistributedElgamal {

    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    private final int KEY_LENGTH;
    private boolean USE_STATIC_PARAMETERS = false;

    public static final BigInteger p256 = new BigInteger(1, new byte[] { 0, -20, -64, 111, 125, 55, 114, 90, 24, -114, -72, 126, 113, 21, 64, 7, 115, 19, -10, -98, -114, 13, 80, -71, -112, 124, -119, 113, 109, -47, 95, -52, 55 });
    public static final BigInteger g256 = new BigInteger(1, new byte[] { 11, -2, 89, -66, -99, 18, -60, 115, -89, -30, 25, -91, -102, -32, -11, -112, -32, -118, -99, 96, 83, -33, 57, 12, 86, 5, 120, -88, -125, 72, 40, 22 });


    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private final Cipher cipher;
    private final SecureRandom random;

    public DistributedElgamal() throws CryptoException {

        this(256);
        this.USE_STATIC_PARAMETERS = true;

    }

    public DistributedElgamal(int key_length) throws CryptoException {

        try {

            this.KEY_LENGTH = key_length;
            cipher = Cipher.getInstance("ElGamal/None/NoPadding", "SC");
            random = new SecureRandom();

        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e) {
            throw new CryptoException("Could not instantiate crypto");
        }

    }


    public PublicKey combinePublicKeys(PublicKey k1, PublicKey k2) throws CryptoException {

        /* Getting the public key values */
        BigInteger y1 = k1.getY();
        BigInteger y2 = k2.getY();

        /* Getting the p and g values */
        BigInteger p = k1.getP();
        BigInteger g = k1.getG();

        /* Multiplying the public keys */
        BigInteger y = y1.multiply(y2).mod(p);

        /* Making a new combined public key from y */
        return new PublicKey(y, p, g);

    }

    public byte[] combineCiphers(byte[] beta1, byte[] beta2, byte[] cipher, KeyParameters parameters) throws CryptoException {

        BigInteger alpha = new BigInteger(1, splitCipher(cipher).first),
                b1 = new BigInteger(1, beta1),
                b2 = new BigInteger(1, beta2),
                p = parameters.getP();

        return alpha.multiply(b1).multiply(b2).mod(p).toByteArray();

    }

    public byte[] partialDecrypt(byte[] cipher, PrivateKey key) throws CryptoException {

        BigInteger x = key.getX();
        BigInteger p = key.getP();

        Pair<byte[], byte[]> pair = splitCipher(cipher);

        BigInteger beta = new BigInteger(1, pair.second);

        return beta.modPow(p.subtract(ONE).subtract(x), p).toByteArray();

    }

    public byte[] encrypt(byte[] message, PublicKey key) throws CryptoException {

        try {
            cipher.init(Cipher.ENCRYPT_MODE, convert(key), random);
            return cipher.doFinal(message);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new CryptoException("Invalid key");
        }

    }

    public byte[] decrypt(byte[] cipherText, PrivateKey key) throws CryptoException {

        try {
            cipher.init(Cipher.DECRYPT_MODE, convert(key));
            return cipher.doFinal(cipherText);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new CryptoException("Invalid key");
        }

    }

    public PrivateKey generateKey() throws CryptoException {

        if (USE_STATIC_PARAMETERS) {

            KeyParameters params = new KeyParameters(p256, g256);
            return generateKey(params);

        } else {

            try {

                KeyPairGenerator generator = KeyPairGenerator.getInstance("ElGamal", "SC");
                generator.initialize(KEY_LENGTH, random);
                return convert(generator.generateKeyPair());

            } catch (Exception e) {
                throw new CryptoException("The key could not be generated");
            }
        }
    }

    public PrivateKey generateKey(KeyParameters spec) throws CryptoException {

        try {

            KeyPairGenerator generator = KeyPairGenerator.getInstance("ElGamal", "SC");
            generator.initialize(convert(spec), random);
            return convert(generator.generateKeyPair());

        } catch (Exception e) {
            throw new CryptoException("The key could not be generated");
        }

    }

    private Pair<byte[], byte[]> splitCipher(byte[] c) {

        byte[] in1 = new byte[c.length / 2];
        byte[] in2 = new byte[c.length / 2];

        System.arraycopy(c, 0, in1, 0, in1.length);
        System.arraycopy(c, in1.length, in2, 0, in2.length);

        BigInteger beta = new BigInteger(1, in1);
        BigInteger alpha = new BigInteger(1, in2);

        return new Pair<>(alpha.toByteArray(), beta.toByteArray());

    }

    private ElGamalPublicKey getPublicKey(java.security.KeyPair keyPair) {
        return (ElGamalPublicKey) keyPair.getPublic();
    }

    private ElGamalPrivateKey getPrivateKey(java.security.KeyPair keyPair) {
        return (ElGamalPrivateKey) keyPair.getPrivate();
    }

    private ElGamalPublicKey convert(PublicKey key) throws CryptoException {

        try {

            KeyFactory kf = KeyFactory.getInstance("ElGamal", "SC");

            BigInteger y = key.getY();
            BigInteger p = key.getP();
            BigInteger g = key.getG();

            ElGamalParameterSpec params = new ElGamalParameterSpec(p, g);
            ElGamalPublicKeySpec spec = new ElGamalPublicKeySpec(y, params);

            return (ElGamalPublicKey) kf.generatePublic(spec);

        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            throw new CryptoException("Invalid key");
        }

    }

    private ElGamalPrivateKey convert(PrivateKey key) throws CryptoException {

        try {

            KeyFactory kf = KeyFactory.getInstance("ElGamal", "SC");

            BigInteger x = key.getX();
            BigInteger p = key.getP();
            BigInteger g = key.getG();

            ElGamalParameterSpec params = new ElGamalParameterSpec(p, g);
            ElGamalPrivateKeySpec spec = new ElGamalPrivateKeySpec(x, params);

            return (ElGamalPrivateKey) kf.generatePrivate(spec);

        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            throw new CryptoException("Invalid key");
        }

    }

    private PrivateKey convert(java.security.KeyPair key) {

        ElGamalPublicKey pub = getPublicKey(key);
        ElGamalPrivateKey priv = getPrivateKey(key);

        BigInteger x = priv.getX();
        BigInteger y = pub.getY();
        BigInteger p = pub.getParameters().getP();
        BigInteger g = pub.getParameters().getG();

        return new PrivateKey(x, y, p, g);

    }

    private ElGamalParameterSpec convert(KeyParameters params) {
        return new ElGamalParameterSpec(params.getP(), params.getG());
    }

    public Challenge createChallenge(PublicKey key) throws CryptoException {

        BigInteger p = key.getP(), pMinusTwo = p.subtract(TWO), challenge;

        do
        {
            BigInteger h = BigIntegers.createRandomInRange(TWO, pMinusTwo, random);

            challenge = h.modPow(TWO, p);
        }
        while (challenge.equals(ONE));

        byte[] nonce = BigIntegers.asUnsignedByteArray(challenge);
        byte[] cipher = encrypt(nonce, key);
        return new Challenge(nonce, cipher);

    }

    @TestOnly
    public boolean sanityCheck() throws CryptoException {

        PrivateKey pk1 = generateKey(), pk2 = generateKey(pk1);
        PublicKey pub = combinePublicKeys(pk1, pk2);

        Challenge challenge = createChallenge(pub);

        byte[] part1 = partialDecrypt(challenge.getCipher(), pk1);
        byte[] part2 = partialDecrypt(challenge.getCipher(), pk2);
        byte[] response = combineCiphers(part1, part2, challenge.getCipher(), pub);

        return challenge.check(response);
    }

}


