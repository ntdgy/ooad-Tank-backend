package tank.ooad.fitgub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.repo.Issue;
import tank.ooad.fitgub.entity.repo.IssueContent;

import java.util.List;


@Component
@Slf4j
public class RepoIssueService {
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Return issue id (serial, pkey)
     *
     * @param ownerName
     * @param repoName
     * @param repoIssueId
     * @return
     */
    public int resolveIssue(String ownerName, String repoName, int repoIssueId) {
        return jdbcTemplate.queryForObject("""
                    select issue.id from issue
                        join repo r on r.id = issue.repo_id
                        join users uo on uo.id = r.owner_id
                    where uo.name = ? and r.name = ? and issue.repo_issue_id = ?;
                """, Integer.class, ownerName, repoName, repoIssueId);
    }

    private int getRepoNextIssueId(int repoId) {
        return jdbcTemplate.queryForObject(
                "update repo set next_issue_id = repo.next_issue_id + 1 where id = ? returning next_issue_id;",
                Integer.class, repoId);
    }
    private int getIssueNextCommentId(int issueId) {
        return jdbcTemplate.queryForObject(
                "update issue set next_comment_id = issue.next_comment_id + 1 where id = ? returning next_comment_id;",
                Integer.class, issueId);
    }

    /***
     * Return generated issue id (serial)
     * @param repoId
     * @param title
     * @param IssuerId
     * @param tag
     * @return
     */
    public int createIssue(int repoId, String title, int IssuerId, String tag) {
        var repoIssueId = jdbcTemplate.queryForObject(
                "update repo set next_issue_id = repo.next_issue_id + 1 where id = ? returning next_issue_id;",
                Integer.class, repoId);
        return jdbcTemplate.queryForObject("""
                        insert into issue(repo_id, repo_issue_id, issuer_user_id, title, tag)
                        values (?,?,?,?,?,?)
                        returning issue.id as id;""",
                Integer.class,
                repoId, repoIssueId, IssuerId, title, tag);
    }

    public int insertIssueContent(int issueId, int senderUserId, IssueContent content) {
        int commentId = getIssueNextCommentId(issueId);
        return jdbcTemplate.queryForObject("""
                insert into issue_content(issue_id, comment_id, sender_user_id, content) 
                values (?,?,?,?) returning id;
                """, Integer.class,
                issueId, commentId, senderUserId, content.content);
    }

    public void closeIssue(int issueId) {
        jdbcTemplate.update("""
                update issue set status = 'closed' where id = ?;
                """, issueId);
    }

    public void reopenIssue(int issueId) {
        jdbcTemplate.update("""
                update issue set status = 'open' where id = ?;
                """, issueId);
    }

    public boolean checkIssueOwner(int currentUserId, int issueId) {
        Integer cnt = jdbcTemplate.queryForObject("""
                select count(*) from issue where issue.id = ? and issue.issuer_user_id = ?
                """, Integer.class, issueId, currentUserId);
        return cnt != null && cnt > 0;
    }

    public boolean checkIssueClosable(int issueId) {
        Integer cnt = jdbcTemplate.queryForObject("""
                select count(*) from issue
                    where issue.id = ? and issue.status = 'open'
                """, Integer.class, issueId);
        return cnt != null && cnt > 0;
    }

//    public Issue getIssue(String ownerName, String repoName, int repoIssueId) {
//        return jdbcTemplate.queryForObject("""
//                select issue.id, issue.repo_issue_id, issue.title, issue.status, issue.tag,
//                       ui.id as issuer_id, ui.name as issuer_name, ui.email as issuer_email
//                from issue
//                    join users ui on issue.issuer_user_id = ui.id
//                where issue.id = (select iss.id
//                            from issue as iss
//                                     join repo r on r.id = iss.repo_id
//                                     join users uo on uo.id = r.owner_id
//                            where uo.name = ?
//                              and r.name = ?
//                              and iss.repo_issue_id = ?)
//                """,
//                Issue.mapper,
//                ownerName, repoName, repoIssueId);
//    }
public Issue getIssue(String ownerName, String repoName, int repoIssueId) {
    return jdbcTemplate.queryForObject("""
                with issue_content as (select iss.id
                                       from issue as iss
                                                join repo r on r.id = iss.repo_id
                                                join users uo on uo.id = r.owner_id
                                       where uo.name = ?
                                         and r.name = ?
                                         and iss.repo_issue_id = ?)
                select issue.id,
                       issue.repo_issue_id,
                       issue.title,
                       issue.status,
                       issue.tag,
                       ui.id    as issuer_id,
                       ui.name  as issuer_name,
                       ui.email as issuer_email
                from issue
                         join users ui on issue.issuer_user_id = ui.id
                    and issue.id in (select id from issue_content);
                """,
            Issue.mapper,
            ownerName, repoName, repoIssueId);
}

    public List<Issue> listIssues(String ownerName, String repoName) {
        return jdbcTemplate.query("""
                with issue_content as (select iss.id
                                       from issue as iss
                                                join repo r on r.id = iss.repo_id
                                                join users uo on uo.id = r.owner_id
                                       where uo.name = ?
                                         and r.name = ?)
                select issue.id,
                       issue.repo_issue_id,
                       issue.title,
                       issue.status,
                       issue.tag,
                       ui.id    as issuer_id,
                       ui.name  as issuer_name,
                       ui.email as issuer_email
                from issue
                         join users ui on issue.issuer_user_id = ui.id
                    and issue.id in (select id from issue_content);
                """,
                Issue.mapper,
                ownerName, repoName);
    }

    public void loadContents(Issue issue) {
        issue.contents = jdbcTemplate.query("""
                select i.id             as content_id,
                       i.issue_id       as issue_id,
                       i.sender_user_id as sender_user_id,
                       u.name           as sender_name,
                       u.email          as sender_email,
                       i.content        as content,
                       i.comment_id     as comment_id
                from issue_content i
                         join users u on i.sender_user_id = u.id
                where issue_id = ?
                order by comment_id;
                        """,IssueContent.mapper,issue.id
        );
    }
}


