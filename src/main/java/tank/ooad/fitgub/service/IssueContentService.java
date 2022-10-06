package tank.ooad.fitgub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.repo.IssueContent;

@Component
public class IssueContentService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public IssueContent getIssueContent(int contentId) {
        return jdbcTemplate.queryForObject("""
                select ic.id as content_id,
                       ic.issue_id,
                       u.id as sender_id,
                       u.name as sender_name,
                       u.email as sender_email,
                       ic.content
                from issue_content ic
                         join users u on ic.sender_user_id = u.id
                where ic.id = ?;
                """, IssueContent.mapper, contentId);
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
