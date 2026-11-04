package lab;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentHelper {

    public record Outcome(int ok, List<Throwable> errors) {}

    public static Outcome runConcurrently(int threads, Runnable task) throws Exception {
        return runConcurrently(threads, task, 30, TimeUnit.SECONDS);
    }

    public static Outcome runConcurrently(int threads, Runnable task, long timeout, TimeUnit unit) throws Exception {
        var pool = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);
        var ok = new AtomicInteger();
        var errors = new ConcurrentLinkedQueue<Throwable>();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    task.run();
                    ok.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(timeout, unit);
        return new Outcome(ok.get(), List.copyOf(errors));
    }
}
