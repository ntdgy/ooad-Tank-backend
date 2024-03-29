package tank.ooad.fitgub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.entity.repo.RepoCollaborator;
import tank.ooad.fitgub.entity.repo.RepoMetaData;
import tank.ooad.fitgub.exception.GitRepoNonExistException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RepoService {

    private final JdbcTemplate template;

    public RepoService(JdbcTemplate template) {
        this.template = template;
    }

    public int resolveRepo(String ownerName, String repoName) {
        Integer id = template.queryForObject("""
                select repo.id from repo
                         join users uo on repo.owner_id = uo.id
                    where uo.name = ? and repo.name = ?;
                """, Integer.class, ownerName, repoName);
        if (id == null) throw new GitRepoNonExistException(ownerName, repoName);
        return id;
    }


    /**
     * Check whether Repo.name is duplicated under namespace userId.
     *
     * @param repo
     * @param userId
     * @return true if duplicated
     */
    public boolean checkRepoDuplicate(String repoName, int userId) {
        int cnt = template.queryForObject("select count(*) from repo where repo.name = ? and repo.owner_id = ?;", Integer.class, repoName, userId);
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
    public List<Repo> getUserRepos(int userId) throws GitRepoNonExistException {
        return template.query("""
                        select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                    repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email,
                                    (select count(*) from star as s where s.user_id = ? and s.repo_id = repo.id) as star,
                                    (select count(*) from watch as w where w.user_id = ? and w.repo_id = repo.id) as watch
                                        from repo
                                    join users uo on repo.owner_id = uo.id
                                where uo.id = ?
                        union distinct
                        select repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                    repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email,
                                    (select count(*) from star as s where s.user_id = ? and s.repo_id = repo.id) as star,
                                    (select count(*) from watch as w where w.user_id = ? and w.repo_id = repo.id) as watch
                                        from repo
                                    join users uo on repo.owner_id = uo.id
                                    join user_repo ur on repo.id = ur.repo_id and ur.user_id = ?
                                    """,
                Repo.mapper,
                userId, userId, userId,
                userId, userId, userId);
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
    public boolean checkCollaboratorReadPermission(String ownerName, String repoName, int currentUserId) {
        Integer cnt = template.queryForObject("""
                            select count(*) from repo join users uo on uo.id = repo.owner_id
                                join user_repo ur on repo.id = ur.repo_id
                             where uo.name = ? and repo.name =? and ur.user_id = ? and permission & ? > 0
                        """, Integer.class,
                ownerName, repoName, currentUserId, RepoCollaborator.COLLABORATOR_READ);
        return cnt != null && cnt > 0;
    }

    public boolean checkCollaboratorWritePermission(String ownerName, String repoName, int currentUserId) {
        Integer cnt = template.queryForObject("""
                            select count(*) from repo join users uo on uo.id = repo.owner_id
                                join user_repo ur on repo.id = ur.repo_id
                             where uo.name = ? and repo.name =? and ur.user_id = ? and permission & ? > 0
                        """, Integer.class,
                ownerName, repoName, currentUserId, RepoCollaborator.COLLABORATOR_WRITE);
        return cnt != null && cnt > 0;
    }

    public boolean checkRepoReadPermission(Repo repo, int currentUserId) {
        if (repo.isPublic()) return true;
        if (repo.owner.id == currentUserId) return true;
        return checkCollaboratorReadPermission(repo.getOwnerName(), repo.getRepoName(), currentUserId);
    }

    public boolean checkRepoWritePermission(Repo repo, int currentUserId) {
        if (repo.owner.id == currentUserId) return true;
        return checkCollaboratorWritePermission(repo.getOwnerName(), repo.getRepoName(), currentUserId);
    }


    public boolean checkRepoOwnerPermission(int currentUserId, String ownerName, String repoName) {
        Integer cnt = template.queryForObject("""
                        select count(*) from repo join users uo on uo.id = repo.owner_id
                         where repo.owner_id = ? and uo.name = ? and repo.name = ?
                         """, Integer.class,
                currentUserId, ownerName, repoName);
        return cnt != null && cnt > 0;
    }

    public List<Repo> getUserPublicRepos(String username, int currentUserId) {
        return template.query("""
                        select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                    repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email,
                                    (select count(*) from star as s where s.user_id = ? and s.repo_id = repo.id) as star,
                                    (select count(*) from watch as w where w.user_id = ? and w.repo_id = repo.id) as watch
                                        from repo
                                    join users uo on repo.owner_id = uo.id
                                where uo.name = ? and repo.visible = ?
                        union distinct
                        select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                    repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email,
                                    (select count(*) from star as s where s.user_id = ? and s.repo_id = repo.id) as star,
                                    (select count(*) from watch as w where w.user_id = ? and w.repo_id = repo.id) as watch
                                        from repo
                                    join users uo on repo.owner_id = uo.id
                                    join user_repo ur on repo.id = ur.repo_id and ur.user_id = (select id from users uu where uu.name = ?)
                                where repo.visible = ?
                        """,
                Repo.mapper,
                currentUserId, currentUserId, username, Repo.VISIBLE_PUBLIC,
                currentUserId, currentUserId, username, Repo.VISIBLE_PUBLIC);
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

    public void removeRepoCollaborator(int ownerUserId, String repoName, int collaboratorUserId) {
        template.update("""
                delete from user_repo where user_repo.repo_id = (select id from repo where repo.name = ? and repo.owner_id = ?) and user_repo.user_id = ?
                """, repoName, ownerUserId, collaboratorUserId);
    }

    public Repo getRepo(int userId, String repoName) {
        try {
            return template.queryForObject("""
                                    select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email
                                from repo join users uo on repo.owner_id = uo.id
                            where repo.owner_id = ? and repo.name = ?""", Repo.mapper,
                    userId, repoName
            );
        } catch (EmptyResultDataAccessException e) {
            throw new GitRepoNonExistException(userId + "", repoName);
        }
    }

    public Repo getRepo(String ownerName, String repoName) {
        try {
            return template.queryForObject("""
                                    select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email
                                from repo join users uo on repo.owner_id = uo.id
                            where uo.name = ? and repo.name = ?""", Repo.mapper,
                    ownerName, repoName
            );
        } catch (EmptyResultDataAccessException e) {
            throw new GitRepoNonExistException(ownerName, repoName);
        }
    }

    public Repo getRepo(int repoId) {
        try {
            return template.queryForObject("""
                                    select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                                repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email
                                from repo join users uo on repo.owner_id = uo.id
                            where repo.id = ?""", Repo.mapper,
                    repoId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new GitRepoNonExistException("", "" + repoId);
        }
    }

    public RepoMetaData getRepoMetaData(Repo repo) {
        var metaData = template.queryForObject("""
                select uo.id               as repo_owner_id,
                       uo.name             as repo_owner_name,
                       uo.email            as repo_owner_email,
                       repo.name           as repo_name,
                       repo.description    as repo_description,
                       repo.stars          as repo_stars,
                       (select count(*) from repo rr where rr.forked_from_id = repo.id) as repo_forks,
                       repo.watchs         as repo_watchers,
                       repo.forked_from_id as forked_from_id,
                       repo."hasPage" as hasPage           
                from repo
                         join users uo on repo.owner_id = uo.id
                where repo.id = ?;
                """, RepoMetaData.mapper, repo.id);
        if (metaData != null && metaData.forked_from_id != 0) {
            metaData.forked_from = getRepo(metaData.forked_from_id);
        }
        return metaData;
    }

    public boolean updateRepoMetaData(Repo repo, RepoMetaData status) {
        if (status.description == null) return false; // Illegal data
        return template.update("""
                update  repo
                set description = ?, forked_from_id = ?
                where repo.id = ?
                """, status.description, status.forked_from_id == 0 ? null : status.forked_from_id, repo.id) == 1;
    }

    public int starRepo(int userId, int repoId) {
        var isStarred = template.queryForObject("""
                        select count(*) from star where user_id = ? and repo_id = ?
                        """, Integer.class,
                userId, repoId);
        if (isStarred == null || isStarred == 0) {
            template.update("""
                    insert into star (user_id, repo_id) values (?, ?)""", userId, repoId);
            var stars = template.queryForObject("""
                    update repo set stars = stars + 1 where id = ?
                    returning stars;
                    """, Integer.class, repoId);
            if (stars == null) {
                throw new RuntimeException("update repo stars failed");
            }
            return stars;
        } else {
            return -1;
        }
    }

    public int watchRepo(int userId, int repoId) {
        var isWatched = template.queryForObject("""
                        select count(*) from watch where user_id = ? and repo_id = ?
                        """, Integer.class,
                userId, repoId);
        if (isWatched == null || isWatched == 0) {
            template.update("""
                    insert into watch (user_id, repo_id) values (?, ?)""", userId, repoId);
            var watchs = template.queryForObject("""
                    update repo set watchs = watchs + 1 where id = ?
                    returning watchs;
                    """, Integer.class, repoId);
            if (watchs == null) {
                throw new RuntimeException("update repo watches failed");
            }
            return watchs;
        } else {
            return -1;
        }
    }

    public int unstarRepo(int userId, int repoId) {
        var isStarred = template.queryForObject("""
                        select count(*) from star where user_id = ? and repo_id = ?
                        """, Integer.class,
                userId, repoId);
        if (isStarred != null && isStarred > 0) {
            template.update("""
                    delete from star where user_id = ? and repo_id = ?
                    """, userId, repoId);
            var stars = template.queryForObject("""
                    update repo set stars = stars - 1 where id = ?
                    returning stars;
                    """, Integer.class, repoId);
            if (stars == null) {
                throw new RuntimeException("update repo stars failed");
            }
            return stars;
        } else {
            return -1;
        }
    }

    public int unwatchRepo(int userId, int repoId) {
        var isWatched = template.queryForObject("""
                        select count(*) from watch where user_id = ? and repo_id = ?
                        """, Integer.class,
                userId, repoId);
        if (isWatched != null && isWatched > 0) {
            template.update("""
                    delete from watch where user_id = ? and repo_id = ?
                    """, userId, repoId);
            var watchs = template.queryForObject("""
                    update repo set watchs = watchs - 1 where id = ?
                    returning watchs;
                    """, Integer.class, repoId);
            if (watchs == null) {
                throw new RuntimeException("update repo watches failed");
            }
            return watchs;
        } else {
            return -1;
        }
    }

    public void setPublic(int repoId) {
        template.update("update repo set visible = ? where id = ?", Repo.VISIBLE_PUBLIC, repoId);
    }

    public void setPrivate(int repoId) {
        template.update("update repo set visible = ? where id = ?", Repo.VISIBLE_PRIVATE, repoId);
    }

    public ArrayList<Boolean> getUserRepoAction(int userId, int repoId) {
        var isStared = template.queryForObject("""
                select count(*) from star where repo_id = ? and user_id = ?;
                """, Integer.class, repoId, userId);
        var isWatched = template.queryForObject("""
                        select count(*) from watch where repo_id = ? and user_id = ?;""",
                Integer.class, repoId, userId);
        var returnList = new ArrayList<Boolean>();
        System.out.println(isStared);
        System.out.println(isWatched);
        returnList.add(isStared != null && isStared > 0);
        returnList.add(isWatched != null && isWatched > 0);
        return returnList;
    }
    public void fillStarAndWatch(Repo repo, int currentUserId) {
        var resultSet = template.queryForRowSet("""
               select (select count(*) from star as s where s.user_id = ? and s.repo_id = repo.id) as star,
                (select count(*) from watch as w where w.user_id = ? and w.repo_id = repo.id) as watch
               from repo where repo.id = ?
               """, currentUserId, currentUserId, repo.id);
        resultSet.next();
        repo.starred = resultSet.getInt("star") != 0;
        repo.watched = resultSet.getInt("watch") != 0;
    }

    public Repo getRandomRepo() {
        return template.queryForObject("""
                select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                       repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email
                from repo
                         join users uo on repo.owner_id = uo.id
                where repo.visible = 0
                order by random()
                limit 1;
                """, Repo.mapper);
    }

    public List<Repo> searchRepo(String keyword){
        return template.query("""
                select repo.id as repo_id, repo.name as repo_name, repo.visible as repo_visible,
                       repo.owner_id as repo_owner_id, uo.name as repo_owner_name, uo.email as repo_owner_email
                from repo
                         join users uo on repo.owner_id = uo.id
                where (repo.name like ? or repo.description like ?) and repo.visible = 0
                """, Repo.mapper, "%" + keyword + "%", "%" + keyword + "%");
    }

    public void setRepoPageStatus(Repo repo, boolean status) {
        template.update("""
                update repo set "hasPage" = ? where id = ?
                """, status, repo.id);
        String url = "https://ooad.dgy.ac.cn/pages/api/configure";
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> map = new HashMap<>();
        String token = "*";
        map.put("token", token);
        map.put("userId",Integer.toString(repo.owner.id));
        map.put("repoId",Integer.toString(repo.id));
        map.put("option",status?"enable":"disable");
        map.put("userName",repo.owner.name);
        map.put("repoName",repo.name);
        var result = restTemplate.postForObject(url, map, String.class);
        System.out.println(result);
    }

    public List<String> getRepoWatchers(Repo repo) {
        var lst = new ArrayList<String>();
        lst.addAll(template.query("""
                select u.email from watch w join users u on w.user_id = u.id where w.repo_id = ?
                """, (rs, rowNum) -> rs.getString("email"), repo.id));
        lst.add(repo.owner.email);
        return lst;
    }
}
