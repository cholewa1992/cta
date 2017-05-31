package cta.auth;

import cta.ServiceException;
import cta.persistence.entities.Challenge;
import cta.persistence.entities.PubKey;
import cta.persistence.entities.User;
import org.bouncycastle.jce.spec.ElGamalParameterSpec;
import org.bouncycastle.jce.spec.ElGamalPublicKeySpec;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.*;
import java.util.Arrays;
import java.util.Date;

import static org.bouncycastle.math.ec.ECConstants.ONE;
import static org.bouncycastle.math.ec.ECConstants.TWO;

/**
 * Created by wismann on 18/04/2017.
 */
@Service
public class ChallengeService {

    private final Cipher cipher;
    private final SecureRandom random;

    @Value("${server.challenge_duration}")
    private long challengeDuration;

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    public ChallengeService() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        cipher = Cipher.getInstance("ElGamal/None/NoPadding", "BC");
        random = new SecureRandom();
    }

    public Challenge generateNewChallenge(User user) throws ServiceException {

        BigInteger p = new BigInteger(Base64.decode(user.getPubKey().getP()));

        BigInteger pMinusTwo = p.subtract(TWO);


        BigInteger challengeBigInt;

        do
        {
            BigInteger h = BigIntegers.createRandomInRange(TWO, pMinusTwo, random);

            challengeBigInt = h.modPow(TWO, p);
        }
        while (challengeBigInt.equals(ONE));

        //BigInteger challengeBigInt = BigInteger.valueOf(Math.abs(random.nextLong()));

        String challengeAsString = String.valueOf(challengeBigInt);
        byte[] input = BigIntegers.asUnsignedByteArray(challengeBigInt);
        log.info("Challenge cleartext bytes: {}", Arrays.toString(input));
        log.info("Challenge cleartext big int: {}", challengeBigInt.toString());

        PublicKey pk = constructPublicKey(user.getPubKey());

        try {
            cipher.init(Cipher.ENCRYPT_MODE, pk, random);
            byte[] cipherBytes = cipher.doFinal(input);
            String cipherText = new String(Base64.encode(cipherBytes));
            Date expireTime = new Date(new Date().getTime() + challengeDuration);
            Challenge challenge = new Challenge(cipherText, challengeAsString, expireTime, user);
            return challenge;
        } catch (Exception e) {
            String em = "An error occured while generating the challenge cipher";
            log.error(em, e);
            throw new ServiceException(em, e);
        }
    }

    private PublicKey constructPublicKey(PubKey pubKey) throws ServiceException {
        BigInteger y = BigIntegers.fromUnsignedByteArray(Base64.decode(pubKey.getY()));
        BigInteger p = BigIntegers.fromUnsignedByteArray(Base64.decode(pubKey.getP()));
        BigInteger g = BigIntegers.fromUnsignedByteArray(Base64.decode(pubKey.getG()));
        // BigInteger y = new BigInteger(Base64.decode(pubKey.getY()));
        // BigInteger p = new BigInteger(Base64.decode(pubKey.getP()));
        // BigInteger g = new BigInteger(Base64.decode(pubKey.getG()));

        try {
            KeyFactory kf = KeyFactory.getInstance("ElGamal", "BC");
            PublicKey k = kf.generatePublic(new ElGamalPublicKeySpec(y, new ElGamalParameterSpec(p,g)));
            return k;
        } catch (Exception e) {
            String em = "An error occured while constructing the public key from the parameters y,g and p";
            log.error(em, e);
            throw new ServiceException(em, e);
        }

    }

}
