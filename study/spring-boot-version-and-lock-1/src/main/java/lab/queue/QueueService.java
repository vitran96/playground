package lab.queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class QueueService {
    private final TicketCounterRepository counterRepo;
    private final TicketRepository ticketRepo;
    private final JobRepository jobRepo;

    public QueueService(TicketCounterRepository counterRepo, TicketRepository ticketRepo, JobRepository jobRepo) {
        this.counterRepo = counterRepo;
        this.ticketRepo = ticketRepo;
        this.jobRepo = jobRepo;
    }

    @Transactional
    public int takeTicketNaive() {
        TicketCounter c = counterRepo.findById(1L).orElseThrow();
        int n = c.getNextNumber();
        sleep(10);
        c.setNextNumber(n + 1);
        ticketRepo.save(new Ticket(n));
        return n;
    }

    @Transactional
    public int takeTicketLocked() {
        TicketCounter c = counterRepo.lockCounter(1L).orElseThrow();
        int n = c.getNextNumber();
        sleep(10);
        c.setNextNumber(n + 1);
        ticketRepo.save(new Ticket(n));
        return n;
    }

    @Transactional
    public boolean workOneNoLock(String worker) {
        Optional<Job> job = jobRepo.claimNextNoLock();
        if (job.isEmpty()) return false;
        sleep(20);
        job.get().setStatus("DONE");
        job.get().setWorker(worker);
        return true;
    }

    @Transactional
    public boolean workOneForUpdate(String worker) {
        Optional<Job> job = jobRepo.claimNextForUpdate();
        if (job.isEmpty()) return false;
        sleep(20);
        job.get().setStatus("DONE");
        job.get().setWorker(worker);
        return true;
    }

    @Transactional
    public boolean workOneSkipLocked(String worker) {
        Optional<Job> job = jobRepo.claimNextSkipLocked();
        if (job.isEmpty()) return false;
        sleep(20);
        job.get().setStatus("DONE");
        job.get().setWorker(worker);
        return true;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
