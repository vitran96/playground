package tech.kingoyster.spring_1.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "hashed_password", nullable = false)
    @JsonIgnore
    private String hashedPassword;

    @Temporal(TemporalType.TIMESTAMP)
    @org.hibernate.annotations.Generated
    @Column(name = "registered_at", insertable = false, updatable = false, length = 19)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime registeredAt;
}
