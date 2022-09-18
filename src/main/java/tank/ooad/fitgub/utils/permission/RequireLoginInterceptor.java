package tank.ooad.fitgub.utils.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tank.ooad.fitgub.utils.AttributeKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class RequireLoginInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PAGE = "/user/login";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod h && h.hasMethodAnnotation(RequireLogin.class)) {
            var session = request.getSession();
            log.info("login state check");
            if ((int) AttributeKeys.USER_ID.getValue(session) == 0) {
                response.sendRedirect(LOGIN_PAGE);
                return false;
            }
        }
        return true;
    }

}
