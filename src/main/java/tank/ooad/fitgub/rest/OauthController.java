package tank.ooad.fitgub.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tank.ooad.fitgub.service.OauthService;
import tank.ooad.fitgub.service.UserService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;

import javax.servlet.http.HttpSession;
import java.util.Objects;

@RestController
public class OauthController {

    @Autowired
    private OauthService oauthService;

    @Autowired
    private UserService userService;

    @GetMapping("/api/oauth/github")
    public Return<Void> github(@RequestParam String code,
                               HttpSession session
                       ) {
        System.out.println(code);
        String accessToken = oauthService.accessToken(code);
        System.out.println(accessToken);
        String userInfo = oauthService.userInfo(accessToken);
        System.out.println(userInfo);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(userInfo);
            int id = node.get("id").asInt();
            String name = node.get("login").asText();
            String email = node.get("email").asText();
            int valid = userService.validateUser(id);
            if (valid == 0) {
                if (Objects.equals(email, "null")){
                    JsonNode emails = oauthService.userEmail(accessToken);
                    for (JsonNode emailNode : emails) {
                        if (emailNode.get("primary").asBoolean()) {
                            email = emailNode.get("email").asText();
                            break;
                        }
                    }
                }
                int userId = userService.createUser(id, name, email);
                AttributeKeys.USER_ID.setValue(session, valid);
                return Return.OK;
            }
            AttributeKeys.USER_ID.setValue(session, valid);
            return Return.OK;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

}

