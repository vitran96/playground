package tech.kingoyster.spring_1.user;

import java.util.List;

public interface UserService {

    List<UserSummary> getAll();
    UserSummary getById(Integer id);
    User create(UserCreateDto user);
    void updatePassword(Integer id, UserPasswordDto userDto);

    void deleteById(Integer id);
}
