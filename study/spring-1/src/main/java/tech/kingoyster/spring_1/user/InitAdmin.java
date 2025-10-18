package tech.kingoyster.spring_1.user;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InitAdmin {
    public final UserRepository userRepository;
    public final PasswordEncoder passwordEncoder;
    public final AdminConfiguration adminConfiguration;

    @EventListener(ApplicationReadyEvent.class)
    void initAdmin() {
        try {
            Optional<UserSummary> admin = userRepository.findOneByEmail(adminConfiguration.getEmail());
            if (admin.isPresent()) {
                return;
            }

            userRepository.save(
                    User.builder()
                            .email(adminConfiguration.getEmail())
                            .fullName(adminConfiguration.getFullName())
                            .hashedPassword(
                                    passwordEncoder.encode(adminConfiguration.getPassword())
                            )
                            .build()
            );
        } catch (Exception e) {
            // TODO: add logger
            System.out.println("Failed to create admin account: " + e.getMessage());
        }
    }
}
