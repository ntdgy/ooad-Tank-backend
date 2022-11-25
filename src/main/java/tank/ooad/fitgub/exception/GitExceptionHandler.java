package tank.ooad.fitgub.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;

import java.util.HashMap;

@Slf4j
@Order(5000)
@RestControllerAdvice(basePackageClasses = {tank.ooad.fitgub.rest.GitController.class, tank.ooad.fitgub.rest.RepoController.class, tank.ooad.fitgub.rest.RepoIssueController.class})
public class GitExceptionHandler {

    @ExceptionHandler(GitRepoNonExistException.class)
    public Return<String> handleEmptyResultDataAccessException(GitRepoNonExistException e) {
        return new Return<>(ReturnCode.GIT_REPO_NON_EXIST, String.format("%s/%s", e.ownerName, e.repoName));
    }

}
