package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.service.UserService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Crypto;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
@RestController
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    @PostMapping("/api/user/login")
    public Return<Void> login(HttpServletResponse response, @RequestBody User login, HttpSession session) {
        if ((int) AttributeKeys.USER_ID.getValue(session) != 0) return new Return<>(ReturnCode.USER_ALREADY_LOGIN);

        log.info(Crypto.hashPassword(login.password));

        int valid = userService.validateUser(login.name, login.email, login.password);
        if (valid == 0) return new Return<>(ReturnCode.USER_AUTH_FAILED);
        AttributeKeys.USER_ID.setValue(session, valid);
        return Return.OK;
    }

    @RequireLogin
    @PostMapping("/api/user/logout")
    public Return<Void> logout(HttpSession session) {
        AttributeKeys.USER_ID.setValue(session, AttributeKeys.USER_ID.getDefaultValue());
        return Return.OK;
    }

    @PostMapping("/api/user/register")
    public Return<Void> createUser(@RequestBody User register) {
        if (userService.checkExist(register.name, register.email)) return new Return<>(ReturnCode.USER_REGISTERED);
        Integer id = jdbcTemplate.queryForObject("insert into users(name, password, email) values (?,?,?) returning id;",
                Integer.class, register.name, Crypto.hashPassword(register.password), register.email);
        jdbcTemplate.update("insert into user_info(user_id, display_name, bio) values (?,?,'');", id, register.name);
        return Return.OK;
    }

    @GetMapping("/api/user/check-login")
    public Return<User> checkLogin(HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (userId == 0)
            return new Return<>(ReturnCode.LOGIN_REQUIRED);
        User user = userService.getUser(userId);
        return new Return<>(ReturnCode.OK, user);
    }
}
