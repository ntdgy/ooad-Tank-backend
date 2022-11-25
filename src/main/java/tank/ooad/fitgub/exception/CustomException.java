package tank.ooad.fitgub.exception;

import tank.ooad.fitgub.utils.ReturnCode;

public class CustomException extends RuntimeException {
    public final ReturnCode returnCode;
    public CustomException(ReturnCode returnCode) {
        super();
        this.returnCode = returnCode;
    }
}
