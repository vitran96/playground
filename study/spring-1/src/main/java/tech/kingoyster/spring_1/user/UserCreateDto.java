package tech.kingoyster.spring_1.user;

import jakarta.validation.constraints.NotNull;
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
    public String email;

    @NotNull
    public String fullName;

    @NotNull
    public String password;
}
