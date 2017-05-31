package cta.auth;

import cta.ServiceException;
import cta.dtos.*;
import cta.persistence.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;


/**
 * Created by wismann on 18/04/2017.
 */
@Controller
public class AuthController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AuthService authService;

    // Check if the username is valid and return server meta data.
    @MessageMapping("/regInfo")
    @SendToUser("/auth/regInfo")
    public Response registerInfo(String username) {
        log.info("/regInfo called");
        try {
            return createResponseSuccess(authService.getRegInfo(username));
        } catch (ServiceException e) {
            return createresponseFailed(e.getMessage());
        }
    }

    // Store the publicKey/user information in DB:
    @MessageMapping("/reg")
    @SendToUser("/auth/reg")
    public Response register(UserPubKey user) {
        log.info("/reg called");
        try {
            return createResponseSuccess(authService.register(user));
        } catch (ServiceException e) {
            return createresponseFailed(e.getMessage());
        }
    }

    // Generate a new challenge based on the public key for the supplied username
    @MessageMapping("/reqChal")
    @SendToUser("/auth/chal")
    public Response requestChallenge(String username) {
        log.info("/reqChal called");
        try {
            return createResponseSuccess(authService.requestChallenge(username));
        } catch (ServiceException e) {
            return createresponseFailed(e.getMessage());
        }
    }

    // Generate a new token if the challenge answer is correct
    @MessageMapping("/ansChal")
    @SendToUser("/auth/token")
    public Response answerChallenge(Answer answer) {
        log.info("/ansChal called");
        try {
            return createResponseSuccess(authService.answerChallenge(answer.getUsername(), answer.getDecChal()));
        } catch (ServiceException e) {
            return createresponseFailed(e.getMessage());
        }
    }

    // Receive a token and either authorize or deny user
    @MessageMapping("/auth")
    @SendToUser("/auth/auth")
    public Response authenticate(String token) {
        log.info("/auth called");

        try {
            long expTimeLeft = authService.authenticate(token);
            String content = "Authenticated successfully";
            return new AuthenticationResponse(true, null, content, expTimeLeft);

        } catch (ServiceException e) {
            return new Response(false, e.getMessage(), null);
        }
    }

    private Response createResponseSuccess(Object responseContent) {
        Response res = new Response();
        res.setSuccess(true);
        res.setErrorMessage(null);
        res.setContent(responseContent);
        return res;
    }

    private Response createresponseFailed(String errorMessage) {
        Response res = new Response();
        res.setSuccess(false);
        res.setErrorMessage(errorMessage);
        res.setContent(null);
        return res;
    }

}