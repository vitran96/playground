package tech.kingoyster.spring_1.user;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public List<UserSummary> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public UserSummary getById(@PathVariable Integer id) {
        return userService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(
            @RequestBody @Valid UserCreateDto userCreateDto
    ) {
        return userService.create(userCreateDto);
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(@PathVariable Integer id, @RequestBody @Valid UserPasswordDto userPasswordDto) {
        userService.updatePassword(id, userPasswordDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Integer id) {
        userService.deleteById(id);
    }
}
