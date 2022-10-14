package tank.ooad.fitgub.entity.repo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.jdbc.core.RowMapper;
import tank.ooad.fitgub.entity.user.User;

import java.util.List;

public class IssueContent {
    @JsonIgnore
    public int content_id;

    @JsonIgnore
    public int issue_id;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE, defaultValue = "0")
    public int issue_content_id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public User sender;

    public long created_at;

    public String content;

//    public List<String> reactions;

    public IssueContent() {
    }

    public IssueContent(int content_id, int issue_id, int issue_content_id, User sender, String content, long created_at) {
        this.content_id = content_id;
        this.issue_id = issue_id;
        this.issue_content_id = issue_content_id;
        this.sender = sender;
        this.content = content;
        this.created_at = created_at;
    }

    public static final RowMapper<IssueContent> mapper = (rs, rowNum) -> {
        return new IssueContent(rs.getInt("content_id"), rs.getInt("issue_id"),
                rs.getInt("comment_id"),
                new User(rs.getInt("sender_user_id"), rs.getString("sender_name"), rs.getString("sender_email")),
                rs.getString("content"),
                rs.getLong("created_at"));
    };
}
