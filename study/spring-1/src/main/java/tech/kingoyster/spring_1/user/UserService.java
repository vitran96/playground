package tech.kingoyster.spring_1.user;

import java.util.List;

public interface UserService {

    List<UserSummary> getAll();
    UserSummary getById(Long id);
    User create(UserCreateDto user);
    void updatePassword(Long id, UserPasswordDto userDto);

    void deleteById(Long id);
}
