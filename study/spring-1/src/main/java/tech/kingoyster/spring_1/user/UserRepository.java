package tech.kingoyster.spring_1.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<UserSummary> findAllProjectedBy();
    Optional<UserSummary> findProjectedById(Long id);

    @Modifying
    @Query("UPDATE User u SET u.hashedPassword = :hashedPassword WHERE u.id = :id")
    void updatePassword(
            @Param("id") Long id,
            @Param("hashedPassword") String hashedPassword
    );
}
