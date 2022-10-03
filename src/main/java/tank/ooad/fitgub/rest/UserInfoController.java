package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.service.UserInfoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Crypto;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;

@RestController
public class UserInfoController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserInfoService userInfoService;

    @RequireLogin
    @PostMapping("/api/userinfo/setgithub")
    public Return<Void> setGithubUser(@RequestParam String githubUserName, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (userId == 0)
            return new Return<>(ReturnCode.LOGIN_REQUIRED);
        userInfoService.setGithubUser(userId, githubUserName);
        return Return.OK;
    }



}
