package tank.ooad.fitgub.entity.git;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GitBlob {
    public String content;
    public boolean isText;
    public long size;

    public GitBlob() {
    }

    public GitBlob(boolean isText, long size) {
        this.isText = isText;
        this.size = size;
    }
}
