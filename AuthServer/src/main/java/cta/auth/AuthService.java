package cta.auth;

import com.fasterxml.jackson.databind.ser.Serializers;
import cta.ServiceException;
import cta.dtos.RegInfo;
import cta.dtos.UserPubKey;
import cta.persistence.entities.Challenge;
import cta.persistence.entities.PubKey;
import cta.persistence.entities.User;
import cta.persistence.repositories.ChallengeRepository;
import cta.persistence.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.rowset.serial.SerialException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.rmi.ServerException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;


/**
 * Created by wismann on 20/04/2017.
 */
@Service
public class AuthService  {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${server.identifier}")
    private String serverId;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ChallengeRepository challengeRepo;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ChallengeService challengeService;


    public RegInfo getRegInfo(String username) throws ServiceException {

        User user = userRepo.findOne(username);

        if (user == null) {
            RegInfo regInfo = new RegInfo(username, serverId);
            return regInfo;
        }
        else {
            String em = "The username: " + username + " is already used on server";
            log.info(em);
            throw new ServiceException(em);
        }
    }

    public User register(UserPubKey user) throws ServiceException {
        // Store the publicKey/user information in DB and return true if successful:

        byte[] pubkey = Base64.getDecoder().decode(user.getPubKeyB64());
        byte[] g = new byte[32];
        byte[] p = new byte[33];
        byte[] y = new byte[pubkey.length - g.length - p.length];

        System.arraycopy(pubkey, 0, y, 0, y.length);
        System.arraycopy(pubkey, y.length, p, 0, p.length);
        System.arraycopy(pubkey, y.length+p.length, g, 0, g.length);

        String yBase64 = Base64.getEncoder().encodeToString(y);
        String pBase64 = Base64.getEncoder().encodeToString(p);
        String gBase64 = Base64.getEncoder().encodeToString(g);

        PubKey pubKeyToSave = new PubKey(yBase64, gBase64, pBase64);
        User userToSave = new User(user.getUsername(), pubKeyToSave);

        User createdUser = userRepo.save(userToSave);
        if (createdUser!= null) {
            log.info("New user saved: {}", createdUser.toString());
            return createdUser;
        }
        else {
            String em = "An error occured on server while storing user information";
            log.error(em);
            throw new ServiceException(em);
        }
    }

    public String requestChallenge(String username) throws ServiceException {
        // Generate a new challenge based on the public key for the supplied username
        User user = userRepo.findOne(username);

        if (user != null) {
            Challenge challenge = challengeService.generateNewChallenge(user);
            Challenge createdChallenge = challengeRepo.save(challenge);

            if (createdChallenge != null) {
                log.info("New challenge saved: {}", createdChallenge.toString());
                return createdChallenge.getChallengeEncrypted();
            }
            else {
                String em = "An error occured on server while storing challenge information";
                log.error(em);
                throw new ServiceException(em);
            }
        }
        else {
            String em = "The user with supplied username: '" + username + "' doesn't exist on server";
            log.info(em);
            throw new ServiceException(em);
        }
    }

    public String answerChallenge(String username, String decChal) throws ServiceException {
        byte[] decChalBytes = Base64.getDecoder().decode(decChal);
        log.info("Challenge response bytes: {}", Arrays.toString(decChalBytes));

        BigInteger bigInteger = new BigInteger(1, decChalBytes);

        log.info("Challenge response bigint: {}", bigInteger.toString());

        Challenge challenge = challengeRepo.findChallengeByDecryption(bigInteger.toString(), username);

        if (challenge != null && challenge.getExpireTime().before(new Date())) {
            String em = "The challenge answered has expired. Please request a new one.";
            log.info(em);
            throw new ServiceException(em);
        }
        else if (challenge != null) {
            String tokenContent = tokenService.generateToken(username);
            log.info("New token generated: {}", tokenContent);
            return tokenContent;
        }
        else {
            String em = "The challenge answer is incorrect. Authentication token can not be granted";
            log.info(em);
            throw new ServiceException(em);
        }
    }

    public long authenticate(String token) throws ServiceException {
        return tokenService.verifyToken(token);
    }


}
