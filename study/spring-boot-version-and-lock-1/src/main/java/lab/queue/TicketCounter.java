package lab.queue;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_counter")
public class TicketCounter {
    @Id
    private Long id;
    private int nextNumber;

    public TicketCounter() {}

    public TicketCounter(Long id, int nextNumber) {
        this.id = id;
        this.nextNumber = nextNumber;
    }

    public Long getId() { return id; }
    public int getNextNumber() { return nextNumber; }
    public void setNextNumber(int nextNumber) { this.nextNumber = nextNumber; }
}
