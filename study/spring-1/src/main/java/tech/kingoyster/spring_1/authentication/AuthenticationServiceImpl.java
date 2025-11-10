package tech.kingoyster.spring_1.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.kingoyster.spring_1.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    
    
}
