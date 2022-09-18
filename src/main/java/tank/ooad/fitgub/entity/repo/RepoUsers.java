package tank.ooad.fitgub.entity.repo;

import tank.ooad.fitgub.entity.user.User;

public class RepoUsers {
    public Repo repo;
    public User user;
    public int permission;

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
