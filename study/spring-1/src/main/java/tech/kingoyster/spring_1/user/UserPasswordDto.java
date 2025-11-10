package tech.kingoyster.spring_1.user;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserPasswordDto {
    @Size(min = 8)
    public String password;
}
