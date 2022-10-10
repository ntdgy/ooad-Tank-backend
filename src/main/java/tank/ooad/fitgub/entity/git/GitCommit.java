package tank.ooad.fitgub.entity.git;

import java.util.Date;

public class GitCommit {
    public String name;
    public String commit_hash;
    public String commit_message;
    public GitPerson committer;
    public GitPerson author;
    public long commit_time;
}
