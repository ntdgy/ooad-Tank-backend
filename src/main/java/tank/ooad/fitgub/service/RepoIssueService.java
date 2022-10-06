package tank.ooad.fitgub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.repo.Issue;


@Component
@Slf4j
public class RepoIssueService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IssueContentService issueContentService;


    /*
     * @param issueId issue id
     * This function is used to get issue by id
     */
    public Issue getIssue(int issueId) {
        return jdbcTemplate.queryForObject("""
                 select i.id     as id,
                        i.repo_issue_id as repo_issue_id,
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
        return issueContentService.insertIssueContent((int) issue.get("id"), (int) issue.get("next_comment_id"), IssuerId, content)
                == 0 ? 0 : (int) issue.get("repo_issue_id");
    }

    public int closeIssue(String ownerName, String repoName, int issueId) {
        try {
            jdbcTemplate.update("""
                    update issue
                    set status = 'closed'
                    where id = (select i.id
                                from issue i
                                         join repo r on i.repo_id = r.id
                                where r.name = ?
                                  and r.owner_id = (select id from users where name = ?)
                                  and i.repo_issue_id = ?);
                    """, repoName, ownerName, issueId);
        } catch (Exception e) {
            log.error(e.getMessage());
            return 0;
        }
        return 1;
    }

    public int reopenIssue(String ownerName, String repoName, int issueId) {
        try {
            jdbcTemplate.update("""
                    update issue
                    set status = 'open'
                    where id = (select i.id
                                from issue i
                                         join repo r on i.repo_id = r.id
                                where r.name = ?
                                  and r.owner_id = (select id from users where name = ?)
                                  and i.repo_issue_id = ?);
                    """, repoName, ownerName, issueId);
        } catch (Exception e) {
            log.error(e.getMessage());
            return 0;
        }
        return 1;
    }


    public boolean checkIssueOwner(int currentUserId, String ownerName, String repoName, int repoIssueId) {
        Integer cnt = jdbcTemplate.queryForObject("""
                select count(*)
                from issue
                         join repo r on issue.repo_id = r.id and r.name = ?
                         join users uo on r.owner_id = uo.id and uo.name = ?
                    where issue.repo_issue_id = ? and issue.issuer_user_id = ?
                """, Integer.class, repoName, ownerName, repoIssueId, currentUserId);
        return cnt != null && cnt > 0;
    }

    public boolean checkIssueClosable(String ownerName, String repoName, int repoIssueId) {
        Integer cnt = jdbcTemplate.queryForObject("""
                select count(*)
                from issue
                         join repo r on issue.repo_id = r.id and r.name = ?
                         join users uo on r.owner_id = uo.id and uo.name = ?
                    where issue.repo_issue_id = ? and issue.status = 'open'
                """, Integer.class, repoName, ownerName, repoIssueId);
        return cnt != null && cnt > 0;
    }
}


