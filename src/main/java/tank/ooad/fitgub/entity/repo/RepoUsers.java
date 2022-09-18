package tank.ooad.fitgub.entity.repo;

import tank.ooad.fitgub.entity.user.User;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class RepoUsers {
    public Repo repo;
    public User user;

    @Min(value = 0)
    @Max(value = 1)
    public int permission;

    public RepoUsers() {
    }

    public RepoUsers(Repo repo, User user, int permission) {
        this.repo = repo;
        this.user = user;
        this.permission = permission;
    }

    /**
     * Owner own the repo. It can read/write repo, change repo settings, and delete repo.
     */
    public static final int REPO_USER_PERMISSION_OWNER = 0;

    /**
     * Contributor can read/write repo, but cannot change settings.
     */
    public static final int REPO_USER_PERMISSION_CONTRIBUTOR = 0;

    /**
     * ReadOnly can only read repo, cannot write repo.
     */
    public static final int REPO_USER_PERMISSION_READONLY = 0;
}
