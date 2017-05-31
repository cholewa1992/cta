package cta.persistence.repositories;

import cta.persistence.entities.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Created by wismann on 19/04/2017.
 */
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    @Query("select c from Challenge c where c.challengeDecrypted = ?1 and c.user.username = ?2" )
    Challenge findChallengeByDecryption(String challengeDec, String username);
}
