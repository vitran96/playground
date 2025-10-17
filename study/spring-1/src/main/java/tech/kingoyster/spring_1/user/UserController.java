package tech.kingoyster.spring_1.user;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public List<UserSummary> getAll() {
        throw new NotImplementedException();
    }

    @GetMapping("/{id}")
    public UserSummary getById(Long id) {
        throw new NotImplementedException();
    }

    @PostMapping
    public User create(UserCreateDto userCreateDto) {
        throw new NotImplementedException();
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(UserPasswordDto userPasswordDto) {
        throw new NotImplementedException();
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(Long id) {
        throw new NotImplementedException();
    }
}
