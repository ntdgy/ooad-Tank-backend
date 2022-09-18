package tank.ooad.fitgub.service;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.entity.repo.RepoUsers;
import tank.ooad.fitgub.entity.user.User;

import java.awt.event.ItemListener;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public void createRepo(Repo repo, int userId) {
        Integer repoId = template.queryForObject("insert into repo(name, visible) values (?,?) returning id;", Integer.class, repo.name, repo.visible);
        assert repoId != null;
        template.update("insert into user_repo(user_id, repo_id, permission) values (?, ?, 0);", userId, repoId);
    }

    public List<RepoUsers> getUserRepos(int userId) {
        return template.query("""
                select repo_id, repo.name as repo_name, repo.visible as repo_visible, user_id, u.name as user_name, u.email as user_email, permission
                from repo
                         join user_repo ur on repo.id = ur.repo_id
                         join users u on ur.user_id = u.id
                where ur.user_id = ?
                """,
                (rs, rowNum) -> {
                    Repo repo = new Repo(rs.getInt("repo_id"), rs.getString("repo_name"), rs.getInt("repo_visible"));
                    User user = new User(rs.getInt("user_id"), rs.getString("user_name"), rs.getString("user_email"));
                    return new RepoUsers(repo, user, rs.getInt("permission"));
                },
                userId);
    }
}
