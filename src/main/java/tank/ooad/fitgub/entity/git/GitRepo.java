package tank.ooad.fitgub.entity.git;

import java.util.List;

public class GitRepo {
    public List<String> branches;
    public List<String> tags;

    public String default_branch;
    public GitCommit head;

}
