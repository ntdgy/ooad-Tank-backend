package tank.ooad.fitgub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RepoIssueService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int createIssue(int repoId, String title, int userId) {
        jdbcTemplate.update("""
                               insert into issue(repo_id, repo_issue_id, issuer_user_id, title)
                               select ?, repo.next_issue_id, ?, ?
                               from repo where repo.id = ?;
                               """, repoId, userId, title, repoId);
        jdbcTemplate.update("""
                               update repo
                               set next_issue_id = next_issue_id + 1
                               where id = ?;
                               """, repoId);
        return 1;
    }

    public int insertComment(int issueId, int userId, String content) {
        jdbcTemplate.update("""
                               insert into issue_comment(issue_id, comment_id, sender_user_id, content)
                               select ?, next_comment_id, ?, ?
                               from issue
                               where id = ?;
                               """, issueId, userId, content, issueId);
        jdbcTemplate.update(""" 
                                  update issue
                                 set next_comment_id = next_comment_id + 1
                                 where id = ?;
                                 """, issueId);
        return 1;
    }

}
