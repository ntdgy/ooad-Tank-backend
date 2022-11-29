package tank.ooad.fitgub.entity.git;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.List;

public class GitCommit {
    public String commit_hash;
    public String commit_message;
    public GitPerson committer;
    public GitPerson author;
    public long commit_time;

    public static class Diff {
        public String file_path;
        public String origin;
        public String current;
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<Diff> diffList = null;
}
