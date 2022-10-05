package tank.ooad.fitgub.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.entity.repo.RepoCollaborator;
import tank.ooad.fitgub.git.GitOperation;
import tank.ooad.fitgub.rest.RepoIssueController;

import java.util.List;

@Component
public class RepoService {

    private final JdbcTemplate template;
    private final RepoIssueController issueController;

    public RepoService(JdbcTemplate template, RepoIssueController repoIssueController) {
        this.template = template;
        this.issueController = repoIssueController;
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
     * @param ownerUserId
     * @return repo id
     */
    public int createRepo(Repo repo, int ownerUserId) {
        Integer repoId = template.queryForObject("insert into repo(name, visible, owner_id) values (?,?, ?) returning id;", Integer.class, repo.name, repo.visible, ownerUserId);
        assert repoId != null;
        return repoId;
    }

    /**
     * List Repos that userId can access, including collaborator
     *
     * @return
     */
    public List<Repo> getUserRepos(int userId) {
        return template.query("""
                        select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                    repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email
                                    from repo join users uo on repo.owner_id = uo.id
                                where uo.id = ?
                        union distinct
                        select repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                    repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email
                                        from repo
                                    join users uo on repo.owner_id = uo.id
                                    join user_repo ur on repo.id = ur.repo_id and ur.user_id = ?
                                
                                                                                 """,
                Repo.mapper,
                userId,
                userId);
    }

    /**
     * Remove repoId from repo and user_repo table
     *
     * @param repoId
     */
    public void dropRepo(int repoId) {
        template.update("delete from issue where repo_id = ?", repoId);
        template.update("delete from repo where id = ?;", repoId);
    }

    public void dropRepo(int ownerId, String repoName) {
        template.update("delete from issue where repo_id = (select repo.id from repo where repo.owner_id = ? and repo.name = ?)", ownerId, repoName);
        template.update("delete from repo where owner_id = ? and name = ?;", ownerId, repoName);
    }


    /**
     * Check RepoPermission
     *
     * @return
     */
    public boolean checkUserRepoReadPermission(String ownerName, String repoName, int currentUserId) {
        Integer isPublic = template.queryForObject("""
                select visible from repo join users uo on uo.id = repo.owner_id where uo.name = ? and repo.name =?;
                """, Integer.class, ownerName, repoName);
        if (isPublic != null && isPublic == Repo.VISIBLE_PUBLIC) {
            return true;
        }
        Integer cnt = template.queryForObject("""
                            select count(*) from repo join uo on uo.id = repo.owner_id
                                join user_repo ur on repo.id = ur.repo_id
                             where uo.name = ? and repo.name =? and ur.user_id = ? and permission & ? > 0
                        """, Integer.class,
                ownerName, repoName, currentUserId, RepoCollaborator.COLLABORATOR_READ);
        return cnt != null && cnt > 0;
    }

    public boolean checkUserRepoWritePermission(String ownerName, String repoName, int currentUserId, int requiredPermission) {
        Integer cnt = template.queryForObject("""
                            select count(*) from repo join uo on uo.id = repo.owner_id
                                join user_repo ur on repo.id = ur.repo_id
                             where uo.name = ? and repo.name =? and ur.user_id = ? and permission & ? > 0
                        """, Integer.class,
                ownerName, repoName, currentUserId, RepoCollaborator.COLLABORATOR_WRITE );
        return cnt != null && cnt > 0;
    }


    public boolean checkUserRepoOwner(int currentUserId, String repoName) {
        Integer cnt = template.queryForObject("""
                            select count(*) from repo where owner_id = ? and name = ?;
                        """, Integer.class,
                currentUserId, repoName);
        return cnt != null && cnt > 0;
    }

    public GitOperation.RepoStore resolveRepo(String ownerName, String repoName) {
        return template.query("""
                        select repo.owner_id as user_id, repo.id as repo_id from repo join users uo on uo.id = repo.owner_id where repo.name = ? and uo.name = ?;
                        """,
                rs -> {
                    rs.next();
                    return new GitOperation.RepoStore(rs.getInt("user_id"), rs.getInt("repo_id"));
                },
                repoName, ownerName);
    }

    public List<Repo> getUserPublicRepos(String username) {
        return template.query("""
                        select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                    repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email
                                    from repo join users uo on repo.owner_id = uo.id
                                where uo.name = ? and repo.visible = ?
                        union distinct
                        select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                    repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email
                                        from repo
                                    join users uo on repo.owner_id = uo.id
                                    join user_repo ur on repo.id = ur.repo_id and ur.user_id = (select id from users uu where uu.name = ?)
                                where repo.visible = ?
                                                                                 """,
                Repo.mapper,
                username, Repo.VISIBLE_PUBLIC,
                username, Repo.VISIBLE_PUBLIC);
    }

    public List<RepoCollaborator> getRepoCollaborators(int ownerUserId, String repoName) {
        return template.query("""
                        select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                    repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email,
                                    user_id, u.name as user_name, u.email as user_email, permission
                        from repo
                                 join users uo on repo.owner_id = uo.id
                                 join user_repo ur on repo.id = ur.repo_id
                                 join users u on ur.user_id = u.id
                                 where repo.name = ? and repo.owner_id = ?;
                                 """,
                RepoCollaborator.mapper,
                repoName, ownerUserId);
    }

    public void addRepoCollaborator(int ownerUserId, String repoName, int collaboratorUserId, int permission) {
        template.update("""
                                insert into user_repo (user_id, repo_id, permission)
                                select ?, repo.id, ? from repo where repo.owner_id = ? and repo.name = ?
                                ON CONFLICT (repo_id, user_id) DO UPDATE
                                  SET permission = ?
                                """, collaboratorUserId, permission, ownerUserId, repoName, permission);
    }
}
