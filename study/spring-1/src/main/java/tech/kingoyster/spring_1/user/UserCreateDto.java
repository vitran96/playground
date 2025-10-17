package tech.kingoyster.spring_1.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserCreateDto {
    public String email;
    public String fullName;
    public String password;
}
