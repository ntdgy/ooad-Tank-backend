package tank.ooad.fitgub.exception;

public class LoginRequiredException extends Exception {

    public final String path;

    public LoginRequiredException(String path) {
        super();
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
