package tank.ooad.fitgub.entity.repo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.jdbc.core.RowMapper;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.utils.MyConfig;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class RepoUsers {
    public Repo repo;
    public User user;

    @JsonIgnore
    public int permission;

    public String gitUrl;

    public RepoUsers() {
    }
//    private boolean isOwner;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isOwner() {
        return (this.permission & REPO_USER_PERMISSION_OWNER) == REPO_USER_PERMISSION_OWNER;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean canWrite() {
        return (this.permission & REPO_USER_PERMISSION_WRITE) == REPO_USER_PERMISSION_WRITE;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean canRead() {
        return (this.permission & REPO_USER_PERMISSION_READ) == REPO_USER_PERMISSION_READ;
    }

    public RepoUsers(Repo repo, User user, int permission) {
        this.repo = repo;
        this.user = user;
        this.permission = permission;
        this.gitUrl = String.format("%s/%s/%s.git", MyConfig.GIT_HTTP_SERVER_BASE, user.name, repo.name);
    }

    public static final RowMapper<RepoUsers> mapper = (rs, rowNum) -> {
        Repo repo = new Repo(rs.getInt("repo_id"), rs.getString("repo_name"), rs.getInt("repo_visible"));
        User user = new User(rs.getInt("user_id"), rs.getString("user_name"), rs.getString("user_email"));
        return new RepoUsers(repo, user, rs.getInt("permission"));
    };

    /**
     * Owner Permission
     */
    public static final int REPO_USER_PERMISSION_OWNER = 4;

    /**
     * Write Permission
     */
    public static final int REPO_USER_PERMISSION_WRITE = 2;

    /**
     * Read Permission
     */
    public static final int REPO_USER_PERMISSION_READ = 1;

    public static final int REPO_USER_PERMISSION_CREATOR = REPO_USER_PERMISSION_OWNER | REPO_USER_PERMISSION_WRITE | REPO_USER_PERMISSION_READ;
}
