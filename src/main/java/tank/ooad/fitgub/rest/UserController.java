package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Crypto;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;

@Slf4j
@RestController
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/api/user/login")
    public Return login(@RequestBody User login, HttpSession session) {
        if ((int) AttributeKeys.USER_ID.getValue(session) != 0) return new Return(ReturnCode.USER_ALREADY_LOGIN);

        log.info(Crypto.hashPassword(login.password));

        int valid = validateUser(login.name, login.email, login.password);
        if (valid == 0) return new Return(ReturnCode.USER_AUTH_FAILED);
        AttributeKeys.USER_ID.setValue(session, valid);
        return Return.OK;
    }

    @RequireLogin
    @PostMapping("/api/user/logout")
    public Return logout(HttpSession session) {
        AttributeKeys.USER_ID.setValue(session, AttributeKeys.USER_ID.getDefaultValue());
        return Return.OK;
    }

    @PostMapping("/api/user/register")
    public Return createUser(@RequestBody User register) {
        if (checkExist(register.name, register.email)) return new Return(ReturnCode.USER_REGISTERED);
        Integer id = jdbcTemplate.queryForObject("insert into users(name, password, email) values (?,?,?) returning id;",
                Integer.class, register.name, Crypto.hashPassword(register.password), register.email);
        jdbcTemplate.update("insert into user_info(user_id, display_name, bio) values (?,?,'');", id, register.name);
        return Return.OK;
    }

    @GetMapping("/api/user/check-login")
    public Return check(HttpSession session) {
        if ((int) AttributeKeys.USER_ID.getValue(session) != 0) return Return.OK;
        return Return.LOGIN_REQUIRED;
    }

    private boolean checkExist(String username, String email) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from users where name = ? or email=?", Integer.class, username, email);
        return count != null && count != 0;
    }

    /**
     * Validate a user with given (username or email) and raw password, and get its userId if valid
     *
     * @param username
     * @param email
     * @param rawPassword
     * @return 0 if invalid, otherwise a valid user id
     */
    private int validateUser(String username, String email, String rawPassword) {
        try {
            if (username != null) {
                Integer id = jdbcTemplate.queryForObject("select id from users where name = ? and password = ?;", Integer.class, username, Crypto.hashPassword(rawPassword));
                if (id != null) return id;
            }
            if (email != null) {
                Integer id = jdbcTemplate.queryForObject("select id from users where email = ? and password = ?;", Integer.class, email, Crypto.hashPassword(rawPassword));
                if (id != null) return id;
            }
            return 0;
        } catch (DataAccessException ig) {
            return 0;
        }
    }
}
