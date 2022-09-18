package tank.ooad.fitgub.utils;


public class Return {

    public final ReturnCode status;
    public final Object data;

    public Return(ReturnCode status, Object data) {
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

    public static final Return OK = new Return(ReturnCode.OK);
    public static final Return LOGIN_REQUIRED = new Return(ReturnCode.LOGIN_REQUIRED);


}