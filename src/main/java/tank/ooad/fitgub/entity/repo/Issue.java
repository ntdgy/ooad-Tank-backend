package tank.ooad.fitgub.entity.repo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import tank.ooad.fitgub.entity.user.User;

import javax.validation.constraints.Null;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Issue {
    @JsonIgnore
    public int id;

    public int repo_issue_id;

    public String title;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public User issuer;
    public List<String> tag;
    public String status;
    public int comment_count;


    public long created_at;
    public long updated_at;

    public List<IssueContent> contents;

    @JsonIgnore
    public int pull_id = 0;
    public PullRequest pull;

    public Issue() {
    }

    public static final RowMapper<Issue> mapper = (rs, rowNum) -> {
        var iss = new Issue();
        iss.id = rs.getInt("id");
        iss.repo_issue_id = rs.getInt("repo_issue_id");
        iss.title = rs.getString("title");
        iss.status = rs.getString("status");
        iss.tag = Arrays.stream(rs.getString("tag").split(",")).filter(StringUtils::isNotEmpty).toList();
        iss.issuer = new User(rs.getInt("issuer_id"),
                rs.getString("issuer_name"), rs.getString("issuer_email"));
        iss.comment_count = rs.getInt("next_comment_id");
        iss.created_at = rs.getLong("created_at");
        iss.updated_at = rs.getLong("updated_at");
        iss.pull_id = rs.getInt("pull_id");
        return iss;
    };
}
