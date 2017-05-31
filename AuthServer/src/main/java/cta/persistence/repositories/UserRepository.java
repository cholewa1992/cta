package cta.persistence.repositories;

import cta.persistence.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by wismann on 19/04/2017.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

}
