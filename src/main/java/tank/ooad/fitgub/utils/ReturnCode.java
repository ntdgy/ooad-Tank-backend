package tank.ooad.fitgub.utils;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ReturnCode {
    OK(200, ""),
    NOT_IMPLEMENTED(-1, "not implemented!"),

    USER_NOTFOUND(-1000, "user is not found."),
    USER_REGISTERED(-1001, "user is already registered." ),

    GitAPIError(-2000, "Git API Error."),
    GitRepoExist(-2001, "Git Repo Exist.");
    public final int code;
    public final String message;

    ReturnCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String toString() {
        return String.format("%d: %s", code, message);
    }

}