package tank.ooad.fitgub.exception;

import org.springframework.core.annotation.Order;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;

@Order(5000)
@RestControllerAdvice()
public class LoginRequiredHandler {
    @ExceptionHandler(LoginRequiredException.class)
    public ResponseEntity<Return<String>> handleLoginRequiredException(LoginRequiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new Return<>(ReturnCode.LOGIN_REQUIRED, e.getPath()));
    }
}
