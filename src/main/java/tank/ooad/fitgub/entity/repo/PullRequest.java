package tank.ooad.fitgub.entity.repo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.jdbc.core.RowMapper;

public class PullRequest {
    @JsonIgnore
    public int id;

    @JsonIgnore public int from_repo_id;
    @JsonIgnore public int to_repo_id;
    public Repo from;
    public Repo to;
    public String from_branch;
    public String to_branch;

    public static final RowMapper<PullRequest> mapper = (rs, rowNum) -> {
        var pr = new PullRequest();
        pr.id = rs.getInt("id");
        pr.from_repo_id = rs.getInt("from_repo_id");
        pr.to_repo_id = rs.getInt("to_repo_id");
        pr.from_branch = rs.getString("from_branch");
        pr.to_branch = rs.getString("to_branch");
        return pr;
    };

}
