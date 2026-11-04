package lab;

import lab.queue.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static lab.ConcurrentHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class Experiment3QueueTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private TicketCounterRepository counterRepo;

    @Autowired
    private TicketRepository ticketRepo;

    @Autowired
    private JobRepository jobRepo;

    @Test
    @DisplayName("Exp 3A: 50 concurrent tickets with pessimistic lock produce numbers 1..50 with no gaps or duplicates")
    void gaplessTicketNumbersWithLock() throws Exception {
        ticketRepo.deleteAll();
        counterRepo.save(new TicketCounter(1L, 1));

        var ticketNumbers = Collections.synchronizedList(new ArrayList<Integer>());
        var out = runConcurrently(50, () -> {
            int ticket = queueService.takeTicketLocked();
            ticketNumbers.add(ticket);
        });

        System.out.println("Exp 3A Locked Tickets -> ok: " + out.ok() + ", errors: " + out.errors().size());

        assertThat(out.ok()).isEqualTo(50);
        assertThat(out.errors()).isEmpty();

        Collections.sort(ticketNumbers);
        assertThat(ticketNumbers).containsExactlyElementsOf(IntStream.rangeClosed(1, 50).boxed().toList());

        TicketCounter counter = counterRepo.findById(1L).orElseThrow();
        assertThat(counter.getNextNumber()).isEqualTo(51);
        assertThat(ticketRepo.count()).isEqualTo(50);
    }

    @Test
    @DisplayName("Exp 3A (Safety Net): naive ticketing violates UNIQUE constraint under concurrency")
    void naiveTicketingHitsUniqueConstraint() throws Exception {
        ticketRepo.deleteAll();
        counterRepo.save(new TicketCounter(1L, 1));

        var out = runConcurrently(20, () -> {
            queueService.takeTicketNaive();
        });

        System.out.println("Exp 3A Naive Tickets (Unique constraint) -> ok: " + out.ok() + ", errors: " + out.errors().size());

        // Under concurrency without lock, duplicate numbers are attempted and fail the V5 UNIQUE constraint
        assertThat(out.errors()).isNotEmpty();
        assertThat(out.errors()).anyMatch(e ->
                e instanceof DataIntegrityViolationException ||
                (e.getCause() != null && e.getCause().getMessage().contains("uq_ticket_number")));
    }

    @Test
    @DisplayName("Exp 3B: Job queue comparison — no lock vs for update vs skip locked")
    void compareJobQueueVariants() throws Exception {
        // --- 1. No Lock: Demonstrates race condition (duplicate job processing) ---
        seedJobs(20);
        long startNoLock = System.currentTimeMillis();
        var duplicatesCount = runWorkers(4, queueService::workOneNoLock);
        long elapsedNoLock = System.currentTimeMillis() - startNoLock;
        System.out.println("Exp 3B [No lock]: finished in " + elapsedNoLock + " ms, processed with potential race/duplicates");

        // --- 2. FOR UPDATE: No duplicates, but workers serialize/wait on the first row ---
        seedJobs(40);
        long startForUpdate = System.currentTimeMillis();
        runWorkers(4, queueService::workOneForUpdate);
        long elapsedForUpdate = System.currentTimeMillis() - startForUpdate;
        long doneCountForUpdate = jobRepo.findAll().stream().filter(j -> "DONE".equals(j.getStatus())).count();
        System.out.println("Exp 3B [FOR UPDATE]: finished in " + elapsedForUpdate + " ms, done jobs: " + doneCountForUpdate);

        // --- 3. SKIP LOCKED: No duplicates, all 4 workers run in parallel, ~4x faster ---
        seedJobs(40);
        long startSkipLocked = System.currentTimeMillis();
        runWorkers(4, queueService::workOneSkipLocked);
        long elapsedSkipLocked = System.currentTimeMillis() - startSkipLocked;
        long doneCountSkipLocked = jobRepo.findAll().stream().filter(j -> "DONE".equals(j.getStatus())).count();
        System.out.println("Exp 3B [SKIP LOCKED]: finished in " + elapsedSkipLocked + " ms, done jobs: " + doneCountSkipLocked);

        assertThat(doneCountSkipLocked).isEqualTo(40);
        // SKIP LOCKED allows 4 workers to process concurrently, while FOR UPDATE serializes them
        System.out.printf("Speedup with SKIP LOCKED: %.2fx faster than FOR UPDATE%n",
                (double) elapsedForUpdate / Math.max(1, elapsedSkipLocked));
    }

    private void seedJobs(int count) {
        jobRepo.deleteAll();
        List<Job> jobs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            jobs.add(new Job("Task " + i));
        }
        jobRepo.saveAll(jobs);
    }

    private int runWorkers(int workerCount, java.util.function.Function<String, Boolean> workMethod) throws Exception {
        var executor = Executors.newFixedThreadPool(workerCount);
        var tasks = new ArrayList<Callable<Integer>>();
        for (int w = 1; w <= workerCount; w++) {
            final String workerName = "Worker-" + w;
            tasks.add(() -> {
                int processed = 0;
                while (workMethod.apply(workerName)) {
                    processed++;
                }
                return processed;
            });
        }
        var futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        int totalProcessed = 0;
        for (var f : futures) {
            totalProcessed += f.get();
        }
        return totalProcessed;
    }
}
