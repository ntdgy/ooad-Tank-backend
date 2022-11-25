package tank.ooad.fitgub.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.jdbc.core.RowMapper;

public class UserInfo {
    @JsonIgnore
    public int user_id;
    public String display_name;
    public String bio;
    public long create_time;
    public String home_page_url;

    public UserInfo() {
    }

    public UserInfo(int user_id, String display_name, String bio, long create_time, String home_page_url) {
        this.user_id = user_id;
        this.display_name = display_name;
        this.bio = bio;
        this.create_time = create_time;
        this.home_page_url = home_page_url;
    }

    public static final RowMapper<UserInfo> mapper = (rs, rowNum)
            -> new UserInfo(rs.getInt("user_id"), rs.getString("display_name"), rs.getString("bio"), rs.getLong("create_time"), rs.getString("url"));
}
