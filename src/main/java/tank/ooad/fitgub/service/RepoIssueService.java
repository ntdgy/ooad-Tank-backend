package tank.ooad.fitgub.service;

import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.repo.Issue;
import tank.ooad.fitgub.entity.repo.IssueContent;
import tank.ooad.fitgub.entity.repo.PullRequest;
import tank.ooad.fitgub.exception.CustomException;
import tank.ooad.fitgub.utils.ReturnCode;

import java.util.List;


@Component
@Slf4j
public class RepoIssueService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RepoService repoService;


    /**
     * Return issue id (serial, pkey)
     *
     * @param ownerName   repo's owner name
     * @param repoName    repo's name
     * @param repoIssueId repo's issue id
     * @return issue id
     */
    public int resolveIssue(String ownerName, String repoName, int repoIssueId) {
        var issueId = jdbcTemplate.queryForObject("""
                    select issue.id from issue
                        join repo r on r.id = issue.repo_id
                        join users uo on uo.id = r.owner_id
                    where uo.name = ? and r.name = ? and issue.repo_issue_id = ?;
                """, Integer.class, ownerName, repoName, repoIssueId);
        return issueId == null ? -1 : issueId;
    }

    private int getRepoNextIssueId(int repoId) {
        var nextIssueId = jdbcTemplate.queryForObject(
                "update repo set next_issue_id = repo.next_issue_id + 1 where id = ? returning next_issue_id;",
                Integer.class, repoId);
        return nextIssueId == null ? -1 : nextIssueId;
    }

    private int getIssueNextCommentId(int issueId) {
        var nextCommentId = jdbcTemplate.queryForObject(
                "update issue set next_comment_id = issue.next_comment_id + 1 " +
                "where id = ? returning next_comment_id;",
                Integer.class, issueId);
        return nextCommentId == null ? -1 : nextCommentId;
    }

    /***
     * Return generated issue id (serial)
     * @param repoId repo id
     * @param title issue title
     * @param IssuerId issue issuer id
     * @param tag issue tag
     * @return pair of <generated issue_id, repo issue id>
     */
    public Pair<Integer, Integer> createIssue(int repoId, String title, int IssuerId, String tag) {
        var repoIssueId = getRepoNextIssueId(repoId);
        return jdbcTemplate.query("""
                        insert into issue(repo_id, repo_issue_id, issuer_user_id, title, tag)
                        values (?,?,?,?,?)
                        returning issue.id, issue.repo_issue_id;""",
                rs -> {
                    rs.next();
                    return new Pair<>(rs.getInt(1), rs.getInt(2));
                },
                repoId, repoIssueId, IssuerId, title, tag);
    }

    public int insertIssueContent(int issueId, int senderUserId, IssueContent content) {
        int commentId = getIssueNextCommentId(issueId);
        jdbcTemplate.queryForObject("""
                        insert into issue_content(issue_id, comment_id, sender_user_id, content)
                        values (?,?,?,?) returning id;
                        """, Integer.class,
                issueId, commentId, senderUserId, content.content);
        return commentId;
    }

    public void closeIssue(int issueId) {
        jdbcTemplate.update("update issue set status = 'closed' where id = ?;", issueId);
    }

    public void reopenIssue(int issueId) {
        jdbcTemplate.update("update issue set status = 'open' where id = ?;", issueId);
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

    public int resolveIssue(int repoId, int repoIssueId) {
        return jdbcTemplate.queryForObject("""
                        select issue.id
                        from issue where issue.repo_id = ? and issue.repo_issue_id = ?;
                        """,
                Integer.class,
                repoId, repoIssueId);
    }

    /**
     * Get Issue with full Contents
     *
     * @param repoId
     * @param repoIssueId
     * @return
     */
    public Issue getIssue(int repoId, int repoIssueId) {
        var iss = jdbcTemplate.queryForObject("""
                        select issue.id,
                               issue.repo_issue_id,
                               issue.title,
                               issue.status,
                               issue.tag,
                               issue.created_at,
                               issue.next_comment_id,
                               issue.pull_id,
                               (select ic.created_at from issue_content ic where ic.issue_id = issue.id order by ic.comment_id desc limit 1) as updated_at,
                               ui.id    as issuer_id,
                               ui.name  as issuer_name,
                               ui.email as issuer_email
                        from issue
                                 join users ui on issue.issuer_user_id = ui.id
                        where issue.repo_id = ?
                          and issue.repo_issue_id = ?;
                            """,
                Issue.mapper,
                repoId, repoIssueId);
        if (iss == null) throw new RuntimeException(); // TODO: Use NotExistException
        loadContents(iss);
        return iss;
    }

    public List<Issue> listIssues(int repoId) {
        return jdbcTemplate.query("""
                        select issue.id,
                               issue.repo_issue_id,
                               issue.title,
                               issue.status,
                               issue.tag,
                               issue.created_at,
                               issue.next_comment_id,
                               issue.pull_id,
                               (select ic.created_at from issue_content ic where ic.issue_id = issue.id order by ic.comment_id desc limit 1) as updated_at,
                               ui.id    as issuer_id,
                               ui.name  as issuer_name,
                               ui.email as issuer_email
                        from issue
                                 join users ui on issue.issuer_user_id = ui.id
                        where issue.repo_id = ? and issue.pull_id is NULL
                        order by issue.id desc;
                                """,
                Issue.mapper,
                repoId);
    }

    public List<Issue> listPulls(int repoId) {
        return jdbcTemplate.query("""
                        select issue.id,
                               issue.repo_issue_id,
                               issue.title,
                               issue.status,
                               issue.tag,
                               issue.created_at,
                               issue.next_comment_id,
                               issue.pull_id,
                               (select ic.created_at from issue_content ic where ic.issue_id = issue.id order by ic.comment_id desc limit 1) as updated_at,
                               ui.id    as issuer_id,
                               ui.name  as issuer_name,
                               ui.email as issuer_email
                        from issue
                                 join users ui on issue.issuer_user_id = ui.id
                        where issue.repo_id = ? and issue.pull_id is not NULL
                        order by issue.id desc;
                                """,
                Issue.mapper,
                repoId);
    }

    /**
     * Get Pull Request with full content
     *
     * @param repoId
     * @param repoPullId
     * @return
     */
    public Issue getPull(int repoId, int repoPullId) {
        var iss = jdbcTemplate.queryForObject("""
                        select issue.id,
                               issue.repo_issue_id,
                               issue.title,
                               issue.status,
                               issue.tag,
                               issue.created_at,
                               issue.next_comment_id,
                               issue.pull_id,
                               (select ic.created_at from issue_content ic where ic.issue_id = issue.id order by ic.comment_id desc limit 1) as updated_at,
                               ui.id    as issuer_id,
                               ui.name  as issuer_name,
                               ui.email as issuer_email
                        from issue
                                 join users ui on issue.issuer_user_id = ui.id
                        where issue.repo_id = ?
                          and issue.repo_issue_id = ?
                          and issue.pull_id is not NULL;
                        """,
                Issue.mapper,
                repoId, repoPullId);
        if (iss == null) throw new RuntimeException(); // TODO
        loadContents(iss);
        loadPull(iss);
        return iss;
    }

    private void loadContents(Issue issue) {
        issue.contents = jdbcTemplate.query("""
                select i.id             as content_id,
                       i.issue_id       as issue_id,
                       i.sender_user_id as sender_user_id,
                       u.name           as sender_name,
                       u.email          as sender_email,
                       i.content        as content,
                       i.comment_id     as comment_id,
                       i.created_at      as created_at
                from issue_content i
                         join users u on i.sender_user_id = u.id
                where issue_id = ?
                order by comment_id;
                """, IssueContent.mapper, issue.id
        );
    }

    private void loadPull(Issue iss) {
        iss.pull = jdbcTemplate.queryForObject("select * from pull_requests where id = ?", PullRequest.mapper, iss.pull_id);
        if (iss.pull == null) throw new CustomException(ReturnCode.SERVER_INTERNAL_ERROR);
        iss.pull.from = repoService.getRepo(iss.pull.from_repo_id);
        iss.pull.to = repoService.getRepo(iss.pull.to_repo_id);
    }

    public void insertPullRequest(int issueId, PullRequest pull) {
        Integer pullId = jdbcTemplate.queryForObject("insert into pull_requests(from_repo_id, to_repo_id, from_branch, to_branch) values (?,?,?,?) returning id;",
                Integer.class, pull.from.id, pull.to.id, pull.from_branch, pull.to_branch);
        jdbcTemplate.update("update issue set pull_id = ? where issue.id=?", pullId, issueId);
    }
}


