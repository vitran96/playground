package tech.kingoyster.spring_1.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Email String username,
        @Size(min = 8) String password) {
}
