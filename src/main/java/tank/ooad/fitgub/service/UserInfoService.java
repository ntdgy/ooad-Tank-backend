package tank.ooad.fitgub.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.eclipse.jgit.lib.ObjectStream;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.entity.user.UserInfo;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
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

    public boolean updateUserHomePageUrl(int userId, String url) {
        jdbcTemplate.update("update user_info set url = ? where user_id = ?", url, userId);
        return true;
    }

    public boolean updateUserBio(int userId, String bio) {
        jdbcTemplate.update("update user_info set bio = ? where user_id = ?", bio, userId);
        return true;
    }

    public FileSystemResource getAvatar(int userId) {
        try {
            System.out.println("getAvatar");

            var avatar = new File(AVATAR_PATH + "/" + userId + ".jpg");
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

    public boolean setAvatar(int userId, MultipartFile avatar) throws IOException {
        try {
            var avatarFile = new File(AVATAR_PATH + "/" + userId);
            Thumbnails.of(avatar.getInputStream()).outputFormat("jpg").size(512, 512).toFile(avatarFile);
        } catch (net.coobird.thumbnailator.tasks.UnsupportedFormatException e) {
            return false;
        }
        return true;
    }

    public UserInfo getUserInfo(int userId) {
        return jdbcTemplate.queryForObject("select * from user_info where user_id = ?", UserInfo.mapper, userId);
    }

}
