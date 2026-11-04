package lab.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query(value = """
        select * from job
        where status = 'NEW'
        order by id
        limit 1
        """, nativeQuery = true)
    Optional<Job> claimNextNoLock();

    @Query(value = """
        select * from job
        where status = 'NEW'
        order by id
        limit 1
        for update
        """, nativeQuery = true)
    Optional<Job> claimNextForUpdate();

    @Query(value = """
        select * from job
        where status = 'NEW'
        order by id
        limit 1
        for update skip locked
        """, nativeQuery = true)
    Optional<Job> claimNextSkipLocked();
}
