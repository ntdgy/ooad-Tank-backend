package tank.ooad.fitgub.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;

@Slf4j
@Order(5000)
@RestControllerAdvice(basePackageClasses = {tank.ooad.fitgub.rest.GitController.class, tank.ooad.fitgub.rest.RepoController.class, tank.ooad.fitgub.rest.RepoIssueController.class})
public class GitExceptionHandler {

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public Return<Void> handleEmptyResultDataAccessException(EmptyResultDataAccessException e) {
        log.info(e.getMessage());
        return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
    }

}
