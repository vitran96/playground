package tech.kingoyster.spring_1.authentication;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.kingoyster.spring_1.user.UserService;
import tech.kingoyster.spring_1.user.UserSummary;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {
    private final UserService userService;

    @GetMapping("/profile")
    public UserSummary getProfile(Principal principal) {
        int userId = Integer.parseInt(principal.getName());
        return userService.getById(userId);
    }
}
