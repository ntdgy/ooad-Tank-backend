package tank.ooad.fitgub.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.user.User;
@Component
public class UserInfoService {
    private final JdbcTemplate jdbcTemplate;


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


    public void setGithubUser(int userId, String github){
        jdbcTemplate.update("update user_info set github = ? where user_id = ?", github, userId);
    }


}