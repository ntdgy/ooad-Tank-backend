package tank.ooad.fitgub.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfo {
    @JsonIgnore
    public int user_id;
    public String display_name;
    public String bio;
}
