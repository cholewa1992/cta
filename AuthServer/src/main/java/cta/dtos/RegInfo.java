package cta.dtos;

/**
 * Created by wismann on 26/04/2017.
 */
public class RegInfo {
    private String username;

    private String serverIdentifier;

    public RegInfo(String username, String serverIdentifier) {
        this.username = username;
        this.serverIdentifier = serverIdentifier;
    }

    @Override
    public String toString() {
        return "RegInfo{" +
                "username='" + username + '\'' +
                ", serverIdentifier='" + serverIdentifier + '\'' +
                '}';
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getServerIdentifier() {
        return serverIdentifier;
    }

    public void setServerIdentifier(String serverIdentifier) {
        this.serverIdentifier = serverIdentifier;
    }
}
