package tech.kingoyster.spring_1.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name="customers")
public class Customers implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Temporal(TemporalType.TIMESTAMP)
    @Generated
    @Column(name="registered_at", insertable = false, updatable = false, length=19)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime registeredAt;

//    public Customers(int id, String name, String email, Timestamp registeredAt) {
//        this.id = id;
//        this.name = name;
//        this.email = email;
//        this.registeredAt = registeredAt;
//    }

//     @Id
//
//
//    @Column(name="id", unique=true, nullable=false)
//    public int getId() {
//        return this.id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }
//
//
//    @Column(name="name", nullable=false, length=255)
//    public String getName() {
//        return this.name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//
//    @Column(name="email", nullable=false, length=255)
//    public String getEmail() {
//        return this.email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
//
//    @Temporal(TemporalType.TIMESTAMP)
//    @Column(name="registered_at", length=19)
//    public Timestamp getRegisteredAt() {
//        return this.registeredAt;
//    }
//
//    public void setRegisteredAt(Timestamp registeredAt) {
//        this.registeredAt = registeredAt;
//    }




}
