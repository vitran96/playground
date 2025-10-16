package tech.kingoyster.spring_1.customer;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;

@Entity
@Table(name="customers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Customer implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Email
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Temporal(TemporalType.TIMESTAMP)
    @Generated
    @Column(name="registered_at", insertable = false, updatable = false, length=19)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime registeredAt;
}
