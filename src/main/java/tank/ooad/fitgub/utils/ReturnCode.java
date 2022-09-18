package tank.ooad.fitgub.utils;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ReturnCode {
    OK(200, ""),
    NOT_IMPLEMENTED(-1, "not implemented!"),
    LOGIN_REQUIRED(-2, "login required"),

    USER_NOTFOUND(-1000, "user is not found."),
    USER_REGISTERED(-1001, "user is already registered."),
    USER_ALREADY_LOGIN(-1002, "already login"),
    USER_AUTH_FAILED(-1003, "auth failed"),

    GitAPIError(-2000, "Git API Error."),
    GitRepoExist(-2001, "Git Repo Exist."),

    REPO_DUPLICATED(-3000, "repo is already existed.");
    public final int code;
    public final String message;

    ReturnCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}