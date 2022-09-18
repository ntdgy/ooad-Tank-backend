package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;

@RequireLogin
@RestController
public class RepoController {

    @Autowired
    private JdbcTemplate template;

    @PostMapping("/api/repo/create")
    public Return create(String name, int visible, HttpSession session) {
        return new Return(ReturnCode.NOT_IMPLEMENTED);
    }
}
