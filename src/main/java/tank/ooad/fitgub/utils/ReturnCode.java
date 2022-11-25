package tank.ooad.fitgub.utils;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ReturnCode {
    OK(200, ""),
    NOT_IMPLEMENTED(-1, "not implemented!"),
    LOGIN_REQUIRED(-2, "login required"),
    ILLEAL_ARGUMENTS(-3, "illegal arguments"),
    SERVER_INTERNAL_ERROR(-500, "server internal error"),
    USER_NOT_FOUND(-1000, "user is not found."),
    USER_REGISTERED(-1001, "user is already registered."),
    USER_ALREADY_LOGIN(-1002, "already login"),
    USER_AUTH_FAILED(-1003, "auth failed"),
    USER_AVATAR_NOTFOUND(-1004, "avatar is not found."),
    USER_AVATAR_SET_FAILED(-1005, "avatar format not supported."),
    USERNAME_OR_EMAIL_EXIST(-1006, "username or email already exist."),
    USER_NOT_EXIST(-1007, "user not exist."),
    USER_INVALID_VERIFY_CODE(-1008, "invalid verify code."),


    GitAPIError(-2000, "Git API Error."),
    GitRepoExist(-2001, "Git Repo Exist."),
    GIT_REPO_NON_EXIST(-2002, "Git repo doesn't exists."),
    GIT_REPO_NO_PERMISSION(-2003, "no permission"),
    GIT_BRANCH_NON_EXIST(-2004, "Requested branch does not exist or it is already set"),
    GIT_FILE_NON_EXIST(-2005, "Requested file doesn't exist."),

    ISSUE_INTERNAL_ERROR(-3000, "some internal error detected, please contact admins"),
    ISSUE_CLOSED(-3001, "issue closed"),
    ISSUE_OPENED(-3002, "issue opened"),

    REPO_DUPLICATED(-4000, "repo is already existed."),
    REPO_NON_EXIST(-4001, "repo is not existed."),
    REPO_NO_PERMISSION(-4002, "no permission"),
    REPO_ALREADY_PUBLIC(-4003, "repo is already public"),
    REPO_ALREADY_PRIVATE(-4004, "repo is already private"),
    REPO_ALREADY_STARRED(-4005, "repo is already starred"),
    REPO_ALREADY_UNSTARRED(-4006, "repo is already unstarred"),
    REPO_ALREADY_WATCHED(-4007, "repo is already watched"),
    REPO_ALREADY_UNWATCHED(-4008, "repo is already unwatched"),
    REPO_ALREADY_FORKED(-4009, "repo is already forked"),
    REPO_ALREADY_UNFORKED(-4010, "repo is already unforked");



    public final int code;
    public final String message;

    ReturnCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}