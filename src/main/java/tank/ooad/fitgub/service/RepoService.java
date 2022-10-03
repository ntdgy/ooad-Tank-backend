package tank.ooad.fitgub.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.entity.repo.RepoUsers;
import tank.ooad.fitgub.git.GitOperation;

import java.util.List;

@Component
public class RepoService {

    private final JdbcTemplate template;

    public RepoService(JdbcTemplate template) {
        this.template = template;
    }

    /**
     * Check whether Repo.name is duplicated under namespace userId.
     *
     * @param repo
     * @param userId
     * @return true if duplicated
     */
    public boolean checkRepoDuplicate(Repo repo, int userId) {
        int cnt = template.queryForObject("select count(*) from repo join user_repo ur on repo.id = ur.repo_id\n" +
                "where repo.name = ? and ur.user_id = ?;", Integer.class, repo.name, userId);
        return cnt != 0;
    }

    /**
     * Insert Repo in database, return generated repoId.
     *
     * @param repo
     * @param userId
     * @return repo id
     */
    public int createRepo(Repo repo, int userId) {
        Integer repoId = template.queryForObject("insert into repo(name, visible) values (?,?) returning id;", Integer.class, repo.name, repo.visible);
        assert repoId != null;
        template.update("insert into user_repo(user_id, repo_id, permission) values (?, ?, ?);", userId, repoId, RepoUsers.REPO_USER_PERMISSION_CREATOR);
        return repoId;
    }

    public List<RepoUsers> getUserRepos(int userId, boolean ownerOnly) {
        int permissionMask = 7;
        if (ownerOnly) permissionMask = 4;
        return template.query("""
                        select repo_id, repo.name as repo_name, repo.visible as repo_visible, user_id, u.name as user_name, u.email as user_email, permission
                        from repo
                                 join user_repo ur on repo.id = ur.repo_id
                                 join users u on ur.user_id = u.id
                        where ur.user_id = ? and permission & ? > 0
                        """,
                RepoUsers.mapper,
                userId, permissionMask);
    }

    /**
     * Remove repoId from repo and user_repo table
     *
     * @param repoId
     */
    public void dropRepo(int repoId) {
        template.update("delete from repo where id = ?;", repoId);
    }

    /**
     * Check RepoPermission
     *
     * @return
     */
    public boolean checkUserRepoPermission(int currentUserId, int repoId, int requiredPermission) {
        Integer cnt = template.queryForObject("""
                            select count(*) from user_repo join users u on u.id = user_repo.user_id join repo r on r.id = user_repo.repo_id
                                where repo_id = ? and u.id = ? and permission & ? = ?
                        """, Integer.class,
                repoId, currentUserId, requiredPermission, requiredPermission);
        return cnt != null && cnt > 0;
    }

    public boolean checkUserRepoOwner(int currentUserId, String reponame) {
        Integer cnt = template.queryForObject("""
                            select count(*) from user_repo join repo r on r.id = user_repo.repo_id
                                where r.name = ? and user_id = ? and permission = 7;
                        """, Integer.class,
                reponame, currentUserId);
        return cnt != null && cnt > 0;
    }

    public GitOperation.RepoStore resolveRepo(String username, String repoName) {
        return template.query("""
                        select repo_id, user_id
                        from user_repo
                                 join users u on u.id = user_repo.user_id and u.name=? and permission = 7
                                 join repo r on r.id = user_repo.repo_id and r.name=?
                        """,
                rs -> {
                    rs.next();
                    return new GitOperation.RepoStore(rs.getInt("user_id"), rs.getInt("repo_id"));
                },
                username, repoName);
    }

    public List<RepoUsers> getUserPublicRepo(String username) {
        return template.query("""
                        select repo_id, repo.name as repo_name, repo.visible as repo_visible, user_id, u.name as user_name, u.email as user_email, permission
                                                from repo
                                                         join user_repo ur on repo.id = ur.repo_id
                                                         join users u on ur.user_id = u.id where u.name = ? and repo.visible = 0 and permission = 7;
                                                         """,
                RepoUsers.mapper,
                username);
    }

    public List<RepoUsers> getRepoPrivilegedUsers(int ownerUserId, String reponame) {
        return template.query("""
                        select repo_id, repo.name as repo_name, repo.visible as repo_visible, user_id, u.name as user_name, u.email as user_email, permission
                                                from repo
                                                         join user_repo ur on repo.id = ur.repo_id and ur.user_id = ?
                                                         join users u on ur.user_id = u.id
                                                         where repo.name = ?;
                                                         """,
                RepoUsers.mapper,
                ownerUserId, reponame);
    }
}
