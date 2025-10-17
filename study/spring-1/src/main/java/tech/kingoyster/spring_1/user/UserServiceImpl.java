package tech.kingoyster.spring_1.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.kingoyster.spring_1.exception.NotFoundException;
import tech.kingoyster.spring_1.exception.UserAlreadyExistsException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCreateDtoMapper userCreateDtoMapper;

    @Override
    public List<UserSummary> getAll() {
        return userRepository.findAllProjectedBy();
    }

    @Override
    public UserSummary getById(Long id) {
        return userRepository.findProjectedById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found!"));
    }

    @Override
    public User create(UserCreateDto userCreateDto) {
        try {
            User user = userCreateDtoMapper.toEntity(userCreateDto);
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("User already exists!", e);
        }
    }

    @Override
    public void updatePassword(Long id, UserPasswordDto userDto) {
        getById(id);
        userRepository.updatePassword(id, passwordEncoder.encode(userDto.getPassword()));
    }

    @Override
    public void deleteById(Long id) {
        getById(id);
        userRepository.deleteById(id);
    }
}
