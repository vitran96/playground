package tech.kingoyster.spring_1.user;

import java.time.LocalDateTime;

public interface UserSummary {
    Integer getId();
    String getFullName();
    String getEmail();
    LocalDateTime getRegisteredAt();
}
