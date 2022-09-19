package tank.ooad.fitgub.entity.git;

import java.util.List;

public class GitCommitTree {
    public String commit_hash;
    public String committer;
    public String committer_email;
    public String author;
    public String author_email;
    public String commit_message;
    public List<GitTreeEntry> tree;

    public GitCommitTree() {

    }
}
