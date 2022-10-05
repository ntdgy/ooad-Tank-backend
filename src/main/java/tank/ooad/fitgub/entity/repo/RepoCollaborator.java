package tank.ooad.fitgub.entity.repo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.jdbc.core.RowMapper;
import tank.ooad.fitgub.entity.user.User;

public class RepoCollaborator {
    public Repo repo;
    public User user;

    @JsonIgnore
    public int permission;

    public RepoCollaborator() {
    }
//    private boolean isOwner;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean canWrite() {
        return (this.permission & COLLABORATOR_WRITE) == COLLABORATOR_WRITE;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean canRead() {
        return (this.permission & COLLABORATOR_READ) == COLLABORATOR_READ;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean getIsOwner() {
        return user.id == repo.owner.id;
    }
    public RepoCollaborator(Repo repo, User user, int permission) {
        this.repo = repo;
        this.user = user;
        this.permission = permission;
    }

    public static final RowMapper<RepoCollaborator> mapper = (rs, rowNum) -> {
        Repo repo = Repo.mapper.mapRow(rs, rowNum);
        User user = new User(rs.getInt("user_id"), rs.getString("user_name"), rs.getString("user_email"));
        return new RepoCollaborator(repo, user, rs.getInt("permission"));
    };

    /**
     * Owner Permission
     */
    public static final int COLLABORATOR_OWNER = 4;

    /**
     * Write Permission
     */
    public static final int COLLABORATOR_WRITE = 2;

    /**
     * Read Permission
     */
    public static final int COLLABORATOR_READ = 1;
}
