package cta.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.interfaces.DecodedJWT;

import cta.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

/**
 * Created by wismann on 20/04/2017.
 */
@Service
public class TokenService {

    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;
    private SecureRandom secureRandom;

    @Value("${server.token_duration}")
    private long tokenDuration;

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    public TokenService() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        secureRandom = new SecureRandom();
        generator.initialize(1024, secureRandom);
        KeyPair pair = generator.generateKeyPair();
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();

        /* Checking that the generated keys are RSA keys */
        if(!(priv instanceof RSAPrivateKey) && !(pub instanceof RSAPublicKey)){
            throw new InvalidKeyException("The public or private key created was faulty!");
        }

        rsaPrivateKey = (RSAPrivateKey) priv;
        rsaPublicKey = (RSAPublicKey) pub;

    }

    public String generateToken(String username) throws ServiceException {

        try{
            Algorithm algorithm = Algorithm.RSA512(rsaPrivateKey);
            Date now = new Date();
            Date expires = new Date(now.getTime() + tokenDuration);

            String token = JWT.create()
                    .withIssuedAt(now)
                    .withExpiresAt(expires)
                    .withClaim("user", username)
                    .sign(algorithm);
            return token;
        }
        catch (Exception e) {
            String em = "An error occured while generating the authentication token";
            log.error(em, e);
            throw new ServiceException(em, e);
        }

    }

    public long verifyToken(String token) throws ServiceException {
        try{
            Algorithm algorithm = Algorithm.RSA512(rsaPublicKey);
            JWTVerifier verifier = JWT.require(algorithm)
                    .build();
            DecodedJWT jwt = verifier.verify(token);

            return jwt.getExpiresAt().getTime() - new Date().getTime();
        }
        catch (InvalidClaimException e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage(), e);
        }
        catch (Exception e) {
            String em = "An error occured while verifying the authentication token";
            log.error(em, e);
            throw new ServiceException(em, e);
        }

    }
}
