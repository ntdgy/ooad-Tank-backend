package tank.ooad.fitgub.entity.user;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.jdbc.core.RowMapper;

public class User {
    @JsonIgnore
    public int id;
    @JsonProperty(defaultValue = "")
    public String name;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, defaultValue = "")
    public String password;
    @JsonProperty(defaultValue = "")
    public String email;

    public User() {
    }

    public User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public static final RowMapper<User> mapper = (rs, rowNum)
            -> new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"));
}
