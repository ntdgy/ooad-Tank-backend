package tank.ooad.fitgub.service;

import org.eclipse.jgit.lib.ObjectStream;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.user.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Component
public class UserInfoService {
    private final JdbcTemplate jdbcTemplate;

    private static final String AVATAR_PATH = "../avatar";

    private static final String DEFAULT_AVATAR = "default.jpg";


    public UserInfoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    /*This function transform user's email to md5
      and set it as default avatar in user_info
      return 1 if successful
    */
    public int setDefaultAvatar(User user) {
        String email = user.email;
        String md5 = DigestUtils.md5Hex(email);
        String url = "https://gravatar.cdn.ntdgy.top/avatar/" + md5 + "?s=200&d=identicon";
        jdbcTemplate.update("update user_info set avatar = ? where user_id = ?", url, user.id);
        return 1;
    }


    public void setGithubUser(int userId, String github) {
        jdbcTemplate.update("update user_info set github = ? where user_id = ?", github, userId);
    }

    public FileSystemResource getAvatar(int userId) {
        try {
            System.out.println("getAvatar");

            var avatar = new File(AVATAR_PATH + "/" + userId);
            if (avatar.exists()) {
                return new FileSystemResource(avatar);
            } else {
                return new FileSystemResource(new File(AVATAR_PATH + "/" + DEFAULT_AVATAR));
            }
        } catch (Exception e) {
            System.out.println(1);
            System.out.println(e.getMessage());
            return new FileSystemResource(new File(AVATAR_PATH + "/" + DEFAULT_AVATAR));
        }

    }

}
