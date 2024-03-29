package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.service.UserService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Crypto;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

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
    @PostMapping("/api/user/updatePassword")
    public Return<Void> updatePassword(@RequestParam String password, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        userService.updatePassword(userId, password);
        return Return.OK;
    }

    @RequireLogin
    @PostMapping("/api/user/updateEmail")
    public Return<Void> updateEmail(@RequestParam String email, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        userService.updateEmail(userId, email);
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

    @PostMapping("/api/user/sendResetPasswordEmail")
    public Return<Void> resetPassword(@RequestParam String email) {
        if (!userService.checkExist(email)) return new Return<>(ReturnCode.USER_NOT_EXIST);
        try {
            userService.sendVerificationCode(userService.findUserByEmail(email).id, email);
        }catch (Exception e){
            return new Return<>(ReturnCode.USER_SEND_EMAIL_FAILED);
        }
        return Return.OK;
    }

    @PostMapping("/api/user/checkVerificationCode")
    public Return<Void> checkVerificationCode(@RequestParam String email, @RequestParam String code) {
        if (!userService.checkExist(email)) return new Return<>(ReturnCode.USER_NOT_EXIST);
        if (!userService.checkVerificationCode(userService.findUserByEmail(email).id, code)) return new Return<>(ReturnCode.USER_INVALID_VERIFY_CODE);
        return Return.OK;
    }

    @PostMapping("/api/user/resetPassword")
    public Return<Void> resetPasswordCode(@RequestParam String email, @RequestParam String code, @RequestParam String password) {
        if (!userService.checkExist(email)) return new Return<>(ReturnCode.USER_NOT_EXIST);
        int userId = userService.findUserByEmail(email).id;
        if (!userService.checkVerificationCode(userId, code)) return new Return<>(ReturnCode.USER_INVALID_VERIFY_CODE);
        userService.updatePassword(userId, password);
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

    @GetMapping("/api/user/{userName}")
    public Return<User> getUser(@PathVariable String userName) {
        User user = userService.findUserByName(userName);
        if (user == null) return new Return<>(ReturnCode.USER_NOT_EXIST);
        return new Return<>(ReturnCode.OK, user);
    }

    @GetMapping("/api/user/{userName}/stars")
    public Return<List<Repo>> getUserStars(@PathVariable String userName) {
        User user = userService.findUserByName(userName);
        if (user == null) return new Return<>(ReturnCode.USER_NOT_EXIST);
        return new Return<>(ReturnCode.OK, userService.getUserStaredRepos(user.id));
    }

    @GetMapping("/api/user/{userName}/watches")
    public Return<List<Repo>> getUserWatches(@PathVariable String userName) {
        User user = userService.findUserByName(userName);
        if (user == null) return new Return<>(ReturnCode.USER_NOT_EXIST);
        return new Return<>(ReturnCode.OK, userService.getUserWatchedRepos(user.id));
    }
}
