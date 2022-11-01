package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tank.ooad.fitgub.service.OauthService;

@RestController
public class OauthController {

    @Autowired
    private OauthService oauthService;

    @GetMapping("/api/oauth/github")
    public void github(@RequestParam String code
                       ) {
        System.out.println(code);
        String accessToken = oauthService.accessToken(code);
        System.out.println(accessToken);
        String userInfo = oauthService.userInfo(accessToken);
        System.out.println(userInfo);
//        System.out.println(oauthService.accessToken(code));

    }

}

