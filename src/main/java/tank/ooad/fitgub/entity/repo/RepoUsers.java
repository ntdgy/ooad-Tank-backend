package tank.ooad.fitgub.entity.repo;

import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.utils.MyConfig;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class RepoUsers {
    public Repo repo;
    public User user;

    @Min(value = 0)
    @Max(value = 1)
    public int permission;

    public String gitUrl;

    public RepoUsers() {
    }

    public RepoUsers(Repo repo, User user, int permission) {
        this.repo = repo;
        this.user = user;
        this.permission = permission;
        this.gitUrl = String.format("%s/%s/%s.git", MyConfig.GIT_HTTP_SERVER_BASE, user.name, repo.name);
    }

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
