package lab.product;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class VersionedProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int stock;

    @Version
    private Long version;

    public VersionedProduct() {}

    public VersionedProduct(String name, int stock) {
        this.name = name;
        this.stock = stock;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public Long getVersion() { return version; }
}
