package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.repo.Issue;
import tank.ooad.fitgub.service.RepoIssueService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import javax.validation.OverridesAttribute;

@RestController
public class RepoIssueController {

    @Autowired
    private RepoIssueService repoIssueService;

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/issue/create")
    public Return<Integer> createIssue(@PathVariable String ownerName, @PathVariable String repoName,
                                       @RequestBody Issue issue,
                                       HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        StringBuilder sb = new StringBuilder();
        if (issue.tag != null) {
            for (String s : issue.tag) {
                sb.append(s);
                sb.append(',');
            }
            sb.deleteCharAt(sb.length() - 1);
        } else sb.append("null");
        int id = repoIssueService.createIssue(ownerName, repoName, issue.title, userId, sb.toString(), issue.initial_content.content);
        return new Return<>(ReturnCode.OK, id);
    }

    @GetMapping("/api/repo/{ownerName}/{repoName}/issue/{repoIssueId}")
    public Return<Void> getIssueContent(@PathVariable String ownerName,
                                        @PathVariable String repoName,
                                        @PathVariable int repoIssueId,
                                        HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        return Return.OK;
    }
}
