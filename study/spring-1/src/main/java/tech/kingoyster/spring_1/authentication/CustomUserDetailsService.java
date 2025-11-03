package tech.kingoyster.spring_1.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tech.kingoyster.spring_1.exception.NotFoundException;
import tech.kingoyster.spring_1.user.User;
import tech.kingoyster.spring_1.user.UserRepository;
import tech.kingoyster.spring_1.user.UserService;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserDetailMapper userDetailMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findOneByEmail(username)
                .orElseThrow(() -> new NotFoundException("User " + username + " not found!"));
        return userDetailMapper.toEntity(user);
    }
}
