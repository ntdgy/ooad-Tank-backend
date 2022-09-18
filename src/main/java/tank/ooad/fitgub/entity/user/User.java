package tank.ooad.fitgub.entity.user;


import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    public int id;
    public String name;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String password;
    public String email;

    public User() {
    }

    public User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
}
