package tech.kingoyster.spring_1.user;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitAdmin {
    public final UserRepository userRepository;
    public final PasswordEncoder passwordEncoder;
    public final AdminConfiguration adminConfiguration;

    @EventListener(ApplicationReadyEvent.class)
    void initAdmin() {
        // Find if any user with email existed.
    }
}
