package tech.kingoyster.spring_1.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDetailMapper {
    UserDetails toEntity(tech.kingoyster.spring_1.user.User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getHashedPassword())
                .build();
    }

//    tech.kingoyster.spring_1.user.User fromEntity(UserDetail userDetail) {
//
//    }
}
