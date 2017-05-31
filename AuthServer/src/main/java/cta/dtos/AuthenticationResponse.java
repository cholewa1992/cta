package cta.dtos;

/**
 * Created by wismann on 29/04/2017.
 */
public class AuthenticationResponse extends Response {
    private long millisecondsBeforeExpiration;

    public AuthenticationResponse(boolean success, String errorMessage, Object content, long millisecondsBeforeExpiration) {
        super(success, errorMessage, content);
        this.millisecondsBeforeExpiration = millisecondsBeforeExpiration;
    }

    public long getMillisecondsBeforeExpiration() {
        return millisecondsBeforeExpiration;
    }

    public void setMillisecondsBeforeExpiration(long millisecondsBeforeExpiration) {
        this.millisecondsBeforeExpiration = millisecondsBeforeExpiration;
    }

    @Override
    public String toString() {
        return "AuthenticationResponse{" +
                "millisecondsBeforeExpiration=" + millisecondsBeforeExpiration +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", content=" + content +
                '}';
    }
}
