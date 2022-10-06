package tank.ooad.fitgub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.repo.Issue;


@Component
public class RepoIssueService {
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /*
     * @param issueId issue id
     * This function is used to get issue by id
     */
    public Issue getIssue(int issueId) {
        return jdbcTemplate.queryForObject("""
                 select i.id     as id,
                        i.title  as title,
                        i.tag    as tag,
                        r.name   as repo_name,
                        u1.id    as issuer_id,
                        u1.name  as issuer_name,
                        u1.email as issuer_email,
                        u2.name  as owner_name,
                        i.status as status
                from issue i
                         join repo r   on i.repo_id = r.id
                         join users u1 on i.issuer_user_id = u1.id
                         join users u2 on r.owner_id = u2.id
                where i.id = ?;
                 """, Issue.mapper, issueId);
    }

    public int createIssue(String ownerName, String repoName, String title, int IssuerId, String tag, String content) {
        var issue = jdbcTemplate.queryForMap("""
                insert into issue(repo_id, repo_issue_id, issuer_user_id, title,tag)
                select r.id, r.next_issue_id, ?, ?,?
                from repo r
                         join users uo on r.owner_id = uo.id
                where r.name = ?
                  and uo.name = ?
                returning issue.id as id, issue.repo_issue_id as repo_issue_id,issue.next_comment_id as next_comment_id;
                                """, IssuerId, title, tag, repoName, ownerName);
        if (issue.get("id") == null || issue.get("repo_issue_id") == null) return 0;
        jdbcTemplate.update("""
                update repo
                set next_issue_id = next_issue_id + 1
                where name = ?;
                """, repoName);
        return insertIssueContent((int) issue.get("id"), (int) issue.get("next_comment_id"), IssuerId, content)
                == 0 ? 0 : (int) issue.get("repo_issue_id");

    }

    public int insertIssueContent(int issueId, int commentId, int userId, String content) {
        Integer issueContentId = jdbcTemplate.queryForObject("""
                insert into issue_content(issue_id, comment_id, sender_user_id, content)
                values (?, ?, ?, ?);
                """, Integer.class, issueId, commentId, userId, content);
        if (issueContentId == null) return 0;
        jdbcTemplate.update("""
                update issue
                set next_comment_id = next_comment_id + 1
                where id = ?;
                """, issueId);
        return issueContentId;
    }

}


