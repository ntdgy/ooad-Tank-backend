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



}