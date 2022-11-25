package tank.ooad.fitgub.entity.repo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.jdbc.core.RowMapper;
import org.jetbrains.annotations.Nullable;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.utils.MyConfig;

import java.util.List;

public class RepoMetaData {
    public User owner;
    public String name;
    public String description;
    public int star;
    public int fork;
    public int watch;
    public List<User> contributors = List.of();

    @JsonIgnore
    public int forked_from_id;

    public boolean hasPage;
    public @Nullable Repo forked_from;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getGitUrl() {
        return String.format("%s/%s/%s.git", MyConfig.GIT_HTTP_SERVER_BASE, owner.name, name);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getPageUrl() {
        if(this.hasPage)
            return String.format("%s/%s/%s.git", MyConfig.PAGE_HTTP_SERVER_BASE, owner.name, name);
        else
            return null;
    }

    public RepoMetaData() {
    }

    public RepoMetaData(User owner, String name, String description, int star, int fork, int watch, int forked_from_id, boolean hasPage) {
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.star = star;
        this.fork = fork;
        this.watch = watch;
        this.forked_from_id = forked_from_id;
        this.hasPage = hasPage;
    }

    public static final RowMapper<RepoMetaData> mapper = (rs, rowNum) -> {
        return new RepoMetaData(
                new User(rs.getInt("repo_owner_id"), rs.getString("repo_owner_name"), rs.getString("repo_owner_email")),
                rs.getString("repo_name"),
                rs.getString("repo_description"),
                rs.getInt("repo_stars"),
                rs.getInt("repo_forks"),
                rs.getInt("repo_watchers"),
                rs.getInt("forked_from_id"),
                rs.getBoolean("hasPage"));
    };
}


