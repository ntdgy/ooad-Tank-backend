package tank.ooad.fitgub.entity.repo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.jdbc.core.RowMapper;
import tank.ooad.fitgub.entity.user.User;

import javax.validation.constraints.Null;
import java.util.List;
import java.util.Map;

public class Issue {
    @JsonIgnore
    public int id;

    public int issue_id;
    public String owner_name;
    public String repo_name;

    public String title;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public User issuer;
    public List<String> tag;
    public String status;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public IssueContent initial_content;

    public Issue() {
    }

    public Issue(int id, int issue_id, String title, String tag, User issuer, String owner_name, String repo_name, String status) {
        this.id = id;
        this.issue_id = issue_id;
        this.title = title;
        if (tag != null)
            this.tag = List.of(tag.split(","));
        this.issuer = issuer;
        this.owner_name = owner_name;
        this.repo_name = repo_name;
        this.status = status;
    }

    public static final RowMapper<Issue> mapper = (rs, rowNum) -> {
        if (rowNum == 0) return null;
        return new Issue(rs.getInt("id"), rs.getInt("repo_issue_id"),
                rs.getString("title"), rs.getString("tag"),
                new User(rs.getInt("issuer_id"),rs.getString("issuer_name"),
                        rs.getString("issuer_email")),
                rs.getString("owner_name"), rs.getString("repo_name"),
                rs.getString("status"));
    };
}
