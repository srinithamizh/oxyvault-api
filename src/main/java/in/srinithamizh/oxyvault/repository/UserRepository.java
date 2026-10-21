package in.srinithamizh.oxyvault.repository;

import in.srinithamizh.oxyvault.dto.UserProfile;
import in.srinithamizh.oxyvault.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("""
            select new in.srinithamizh.oxyvault.dto.UserProfile(
                u.id, concat(u.firstName, ' ', u.lastName), u.username, u.email, u.role
            )
            from User u
            order by u.username
            """)
    Page<UserProfile> findAllUserProfiles(Pageable pageable);

    /**
     * Revokes all tokens issued to the user. Must be called inside a transaction.
     *
     * @return number of rows updated (0 if the user does not exist)
     */
    @Modifying
    @Query("update User u set u.tokenVersion = u.tokenVersion + 1 where u.username = :username")
    int incrementTokenVersion(@Param("username") String username);
}
