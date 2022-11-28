package tank.ooad.fitgub.entity.git;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tank.ooad.fitgub.entity.repo.Issue;
import tank.ooad.fitgub.entity.repo.Repo;

import java.util.ArrayList;
import java.util.List;

public class GitMergeStatus {
    public Status status;
    public Repo from;
    public String from_branch;
    @JsonIgnore
    public String from_branch_commit;
    public Repo to;
    public String to_branch;
    @JsonIgnore
    public String to_branch_commit;
    public List<String> conflict_files = new ArrayList<>();

    public GitMergeStatus(Issue pull) {
        status = Status.PENDING;
        from = pull.pull.from;
        from_branch = pull.pull.from_branch;
        to = pull.pull.to;
        to_branch = pull.pull.to_branch;
    }

    public enum Status {
        PENDING,
        READY,
        CONFLICT,
        BRANCH_DELETED,
        MERGED,
        ;
    }
}
