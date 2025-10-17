package tech.kingoyster.spring_1.user;

import java.time.LocalDateTime;

public interface UserSummary {
    Long getId();
    String getFullName();
    String getEmail();
    LocalDateTime getRegisteredAt();
}
