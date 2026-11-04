package lab.queue;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String payload;
    private String status;
    private String worker;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Job() {}

    public Job(String payload) {
        this.payload = payload;
        this.status = "NEW";
    }

    public Long getId() { return id; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWorker() { return worker; }
    public void setWorker(String worker) { this.worker = worker; }
    public Instant getCreatedAt() { return createdAt; }
}
