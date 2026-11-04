package lab.queue;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketCounterRepository extends JpaRepository<TicketCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from TicketCounter c where c.id = :id")
    Optional<TicketCounter> lockCounter(@Param("id") long id);
}
