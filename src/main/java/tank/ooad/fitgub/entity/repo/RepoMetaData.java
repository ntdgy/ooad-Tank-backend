package tank.ooad.fitgub.entity.repo;

import org.springframework.jdbc.core.RowMapper;
import tank.ooad.fitgub.entity.user.User;

import java.util.List;

public class RepoMetaData {
    public User owner;
    public String name;
    public String description;
    public int star;
    public int fork;
    public int watch;
    public List<User> contributors;
    public String fork_from_owner;
    public String fork_from_name;

    public RepoMetaData() {
    }

    public RepoMetaData(String description, int star, int fork, int watch) {
        this.description = description;
        this.star = star;
        this.fork = fork;
        this.watch = watch;
    }

    public static final RowMapper<RepoMetaData> mapper = (rs, rowNum) -> {
        return new RepoMetaData(rs.getString("repo_description"),
                rs.getInt("repo_stars"),
                rs.getInt("repo_forks"),
                rs.getInt("repo_watchers"));
    };
}


