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
    GIT_REPO_NON_EXIST(-2002, "Git repo doesn't exists."),
    GIT_REPO_NO_PERMISSION(-2003, "no permission"),

    ISSUE_INTERNAL_ERROR(-3000, "some internal error detected, please contact admins"),
    ISSUE_CLOSED(-3001, "issue closed"),
    ISSUE_OPENED(-3002, "issue opened"),

    REPO_DUPLICATED(-3000, "repo is already existed."),
    REPO_NON_EXIST(-3001, "repo is not existed."),
    REPO_NO_PERMISSION(-3002, "no permission"),
    REPO_ALREADY_PUBLIC(-3003, "repo is already public"),
    REPO_ALREADY_PRIVATE(-3004, "repo is already private"),
    REPO_ALREADY_STARRED(-3005, "repo is already starred"),
    REPO_ALREADY_UNSTARRED(-3006, "repo is already unstarred"),
    REPO_ALREADY_WATCHED(-3007, "repo is already watched"),
    REPO_ALREADY_UNWATCHED(-3008, "repo is already unwatched"),
    REPO_ALREADY_FORKED(-3009, "repo is already forked"),
    REPO_ALREADY_UNFORKED(-3010, "repo is already unforked");



    public final int code;
    public final String message;

    ReturnCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}