package tank.ooad.fitgub.utils.permission;

import lombok.extern.slf4j.Slf4j;
import tank.ooad.fitgub.utils.AttributeKeys;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequireLogin
@Slf4j
public class RequireLoginFilter implements Filter {

    private static final String LOGIN_PAGE = "/user/login";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        var session = ((HttpServletRequest) servletRequest).getSession();
        log.info("login state check");
        if ((int) AttributeKeys.USER_ID.getValue(session) == 0) {
            response.sendRedirect(LOGIN_PAGE);
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
