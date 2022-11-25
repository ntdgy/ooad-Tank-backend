package tank.ooad.fitgub.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;

@Order(5000)
@RestControllerAdvice()
public class GlobalHandler {
    @ExceptionHandler(LoginRequiredException.class)
    public ResponseEntity<Return<String>> handleLoginRequiredException(LoginRequiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new Return<>(ReturnCode.LOGIN_REQUIRED, e.getPath()));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Return<Void>> handleCustomException(CustomException e) {
        if (e.returnCode == ReturnCode.SERVER_INTERNAL_ERROR) return ResponseEntity.internalServerError().build();
        return ResponseEntity.ok(new Return<>(e.returnCode));
    }
}
