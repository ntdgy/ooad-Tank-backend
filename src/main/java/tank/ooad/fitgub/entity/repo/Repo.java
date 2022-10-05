package tank.ooad.fitgub.entity.repo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.utils.MyConfig;

import javax.validation.constraints.*;

import static tank.ooad.fitgub.validator.ConstantRegexValidator.REPO_NAME;

public class Repo {

    @JsonIgnore
    @Null
    public int id;

    @NotNull
    @Pattern(regexp = REPO_NAME)
    public String name;

    public User owner;

    @JsonIgnore
    public int visible;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    public void setPublic(boolean isPublic) {
        if (isPublic) visible = VISIBLE_PUBLIC;
        else visible = VISIBLE_PRIVATE;
    }

    public boolean getPublic() {
        return visible == VISIBLE_PUBLIC;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getGitUrl() {
        return String.format("%s/%s/%s.git", MyConfig.GIT_HTTP_SERVER_BASE, owner.name, name);
    }

    public Repo() {
    }

    public Repo(int id, String name, int visible) {
        this.id = id;
        this.name = name;
        this.visible = visible;
    }

    public static final int VISIBLE_PUBLIC = 0;
    public static final int VISIBLE_PRIVATE = 1;

    public static final RowMapper<Repo> mapper = (rs, rowNum) -> {
        Repo repo = new Repo(rs.getInt("repo_id"), rs.getString("repo_name"), rs.getInt("repo_visible"));
        repo.owner = new User(rs.getInt("repo_owner_id"), rs.getString("repo_owner_name"), rs.getString("repo_owner_email"));
        return repo;
    };
}
