package tank.ooad.fitgub.utils;


public class Return<T> {

    public final ReturnCode status;
    public final T data;

    public Return(ReturnCode status, T data) {
        this.status = status;
        this.data = data;
    }

    public Return(ReturnCode status) {
        this.status = status;
        this.data = null;
    }

    public String toString() {
        return String.format("%s: %s", status, data);
    }

    public static final Return<Void> OK = new Return<>(ReturnCode.OK);
    public static final Return<Void> LOGIN_REQUIRED = new Return<>(ReturnCode.LOGIN_REQUIRED);


}