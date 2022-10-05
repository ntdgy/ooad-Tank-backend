package tank.ooad.fitgub.entity.repo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.*;

import static tank.ooad.fitgub.validator.ConstantRegexValidator.REPO_NAME;

public class Repo {

    @JsonIgnore
    @Null
    public int id;

    @NotNull
    @Pattern(regexp = REPO_NAME)
    public String name;

    @Min(value = 0)
    @Max(value = 1)
    public int visible;

    public Repo() {
    }

    public Repo(int id, String name, int visible) {
        this.id = id;
        this.name = name;
        this.visible = visible;
    }

    public static final int VISIBLE_PUBLIC = 0;
    public static final int VISIBLE_PRIVATE = 1;
}
