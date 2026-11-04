package lab.queue;

import jakarta.persistence.*;

@Entity
@Table(name = "ticket")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int number;

    public Ticket() {}

    public Ticket(int number) {
        this.number = number;
    }

    public Long getId() { return id; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
}
