package lab.account;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class Account {
    @Id
    private Long id;
    private int balance;

    public Account() {}

    public Account(Long id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public Long getId() { return id; }
    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
}
