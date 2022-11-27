package tank.ooad.fitgub.entity.repo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.jdbc.core.RowMapper;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.utils.MyConfig;

import javax.validation.constraints.*;

import java.sql.SQLException;

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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean starred;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean watched;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    public void setPublic(boolean isPublic) {
        if (isPublic) visible = VISIBLE_PUBLIC;
        else visible = VISIBLE_PRIVATE;
    }

    public boolean isPublic() {
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
        try {
            repo.starred = rs.getInt("star") != 0;
            repo.watched = rs.getInt("watch") != 0;
        } catch (SQLException ignored) {}
        return repo;
    };

    public String getOwnerName() {
        return this.owner.name;
    }

    public String getRepoName() {
        return this.name;
    }
}
