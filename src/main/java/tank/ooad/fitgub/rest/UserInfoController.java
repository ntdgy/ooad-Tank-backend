package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.service.UserInfoService;
import tank.ooad.fitgub.service.UserService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Crypto;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@Slf4j
public class UserInfoController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private UserService userService;

    @RequireLogin
    @PostMapping("/api/userinfo/setGithub")
    public Return<Void> setGithubUser(@RequestParam String githubUserName, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (userId == 0)
            return new Return<>(ReturnCode.LOGIN_REQUIRED);
        userInfoService.setGithubUser(userId, githubUserName);
        return Return.OK;
    }

    @GetMapping(value = "/api/userinfo/{userName}/avatar", produces = "image/jpg")
    public ResponseEntity<FileSystemResource> getAvatar(@PathVariable String userName) {
        User user = userService.findUser(userName);
        if (user == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok().body(userInfoService.getAvatar(user.id));
    }

    @RequireLogin
    @PostMapping("/api/userinfo/{userName}/setAvatar")
    public Return<Void> setAvatar(@PathVariable String userName, @RequestPart MultipartFile avatar, HttpSession session) throws IOException {
        if (avatar.isEmpty())
            return new Return<>(ReturnCode.USER_AVATAR_NOTFOUND);
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (userId == 0)
            return new Return<>(ReturnCode.LOGIN_REQUIRED);
        User user = userService.findUser(userName);
        if (user == null)
            return new Return<>(ReturnCode.USER_NOTFOUND);
        if (user.id != userId)
            return new Return<>(ReturnCode.USER_AUTH_FAILED);
        Path path = Paths.get("../avatar/" + UUID.randomUUID());
        log.error(avatar.getContentType());
        var success = userInfoService.setAvatar(userId, avatar);
        if (success)
            return Return.OK;
        else
            return new Return<>(ReturnCode.USER_AVATAR_SET_FAILED);
    }

}
