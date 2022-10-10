package tank.ooad.fitgub.entity.git;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GitTreeEntry {
    public String name;
    @JsonProperty
    public boolean isFolder(){
        return name.endsWith("/");
    }

    public GitTreeEntry(String name) {
        this.name = name;
    }
}
