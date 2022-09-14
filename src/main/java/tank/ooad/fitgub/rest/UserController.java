package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.slf4j.LoggerFactory;
import tank.ooad.fitgub.entity.User;
import tank.ooad.fitgub.utils.Crypto;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;

import javax.servlet.http.HttpSession;

@RestController
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/api/user/login")
    public Return login(@RequestBody User login, HttpSession session) {
        return new Return(ReturnCode.NOT_IMPLEMENTED);
    }


    @PostMapping("/api/user/register")
    public Return createUser(@RequestBody User register) {
        if (checkExist(register.name, register.email))
            return new Return(ReturnCode.USER_REGISTERED);
        Integer id = jdbcTemplate.queryForObject(
                "insert into users(name, password, email) values (?,?,?) returning id;",
                Integer.class,
                register.name, Crypto.hashPassword(register.password), register.email);
        jdbcTemplate.update("insert into user_info(user_id, display_name, bio) values (?,?,'');", id, register.name);
        return new Return(ReturnCode.OK);
    }

    private boolean checkExist(String username, String email) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where name = ? or email=?",
                Integer.class, username, email);
        return count != null && count != 0;
    }

    /**
     * Validate a user with given (username or email) and raw password, and get its userId
     *
     * @param username
     * @param email
     * @param rawPassword
     * @return 0 if invalid, otherwise a valid user id
     */
    private int validateUser(String username, String email, String rawPassword) {
        if (username != null) {
            Integer id = jdbcTemplate.queryForObject(
                    "select id from users where name = ? and password = ?;",
                    new String[]{username, Crypto.hashPassword(rawPassword)},
                    Integer.class
            );
            if (id == null) return 0;
            else return id;
        } else if (email != null) {
            Integer id = jdbcTemplate.queryForObject(
                    "select id from users where email = ? and password = ?;",
                    new String[]{email, Crypto.hashPassword(rawPassword)},
                    Integer.class
            );
            if (id == null) return 0;
            else return id;
        } else throw new IllegalArgumentException("username and email cannot be null either");
    }
}
