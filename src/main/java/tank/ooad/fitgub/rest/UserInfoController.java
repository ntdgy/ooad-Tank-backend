package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Crypto;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;

import javax.servlet.http.HttpSession;

@RestController
public class UserInfoController {

    @Autowired
    private JdbcTemplate jdbcTemplate;


}
