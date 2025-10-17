package tech.kingoyster.spring_1.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCreateDtoMapper {
    private final PasswordEncoder passwordEncoder;

    public User toEntity(UserCreateDto userCreateDto) {
        return User.builder()
                .email(userCreateDto.getEmail())
                .fullName(userCreateDto.getFullName())
                .hashedPassword(passwordEncoder.encode(userCreateDto.getPassword()))
                .build();
    }
}
