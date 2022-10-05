package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tank.ooad.fitgub.service.RepoIssueService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;

@RestController
public class RepoIssueController {

    @Autowired
    private RepoIssueService repoIssueService;

    @PostMapping("/api/repo/issue/create")
    @RequireLogin
    public Return<Void> createIssue(@RequestParam int repoId, @RequestParam String title, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (userId == 0)
            return new Return<>(ReturnCode.LOGIN_REQUIRED);
        repoIssueService.createIssue(repoId, title, userId);
        return Return.OK;
    }

    @PostMapping("/api/repo/issue/addComment")
    @RequireLogin
    public Return<Void> addComment(@RequestParam int issueId, @RequestParam String content, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (userId == 0)
            return new Return<>(ReturnCode.LOGIN_REQUIRED);
        repoIssueService.insertComment(issueId, userId, content);
        return Return.OK;
    }


}
