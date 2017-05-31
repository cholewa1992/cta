package cta.persistence.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by wismann on 19/04/2017.
 */
@Entity
@Table(name = "Challenge")
public class Challenge implements Serializable {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @Column(name = "chal_enc")
    private String challengeEncrypted;

    @Column(name = "chal_dec")
    private String challengeDecrypted;

    @Column(name = "expire_time")
    private Date expireTime;

    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "user_id")
    private User user;

    //jpa required constructor
    protected Challenge() {
    }

    public Challenge(String challengeEncrypted, String challengeDecrypted, Date expireTime, User user) {
        this.challengeEncrypted = challengeEncrypted;
        this.challengeDecrypted = challengeDecrypted;
        this.expireTime = expireTime;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChallengeEncrypted() {
        return challengeEncrypted;
    }

    public void setChallengeEncrypted(String challengeEncrypted) {
        this.challengeEncrypted = challengeEncrypted;
    }

    public String getChallengeDecrypted() {
        return challengeDecrypted;
    }

    public void setChallengeDecrypted(String challengeDecrypted) {
        this.challengeDecrypted = challengeDecrypted;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Challenge{" +
                "id=" + id +
                ", challengeEncrypted='" + challengeEncrypted + '\'' +
                ", challengeDecrypted='" + challengeDecrypted + '\'' +
                ", expireTime=" + expireTime +
                ", user=" + user +
                '}';
    }
}
