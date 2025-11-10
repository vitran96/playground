package tech.kingoyster.spring_1.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserCreateDto {
    @NotNull
    @Email
    public String email;

    @NotNull
    public String fullName;

    @NotNull
    @Size(min = 8)
    public String password;
}
