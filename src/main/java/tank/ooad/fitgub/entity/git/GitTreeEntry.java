package tank.ooad.fitgub.entity.git;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GitTreeEntry {
    public String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public GitCommit modify_commit;
    @JsonProperty
    public boolean isFolder(){
        return name.endsWith("/");
    }

    public GitTreeEntry(String name) {
        this.name = name;
    }
}
