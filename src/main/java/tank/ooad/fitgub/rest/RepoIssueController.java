package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.repo.Issue;
import tank.ooad.fitgub.service.RepoIssueService;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;

@RestController
public class RepoIssueController {

    @Autowired
    private RepoIssueService repoIssueService;
    @Autowired
    private RepoService repoService;

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

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/issue/{repoIssueId}/close")
    public Return<Integer> closeIssue(@PathVariable String ownerName, @PathVariable String repoName, @PathVariable int repoIssueId,
                                      HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        if (!repoService.checkUserRepoOwner(currentUserId, ownerName, repoName) &&
                !repoService.checkCollaboratorWritePermission(ownerName, repoName, currentUserId) &&
            !repoIssueService.checkIssueOwner(currentUserId, ownerName, repoName, repoIssueId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        if(!repoIssueService.checkIssueClosable(ownerName, repoName, repoIssueId)) {
            return new Return<>(ReturnCode.ISSUE_CLOSED);
        }
        if( repoIssueService.closeIssue(ownerName, repoName, repoIssueId)==1)
            return new Return<>(ReturnCode.OK,repoIssueId);
        else return new Return<>(ReturnCode.ISSUE_INTERNAL_ERROR);
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/issue/{repoIssueId}/reopen")
    public Return<Integer> reopenIssue(@PathVariable String ownerName, @PathVariable String repoName, @PathVariable int repoIssueId,
                                      HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        if (!repoService.checkUserRepoOwner(currentUserId, ownerName, repoName) &&
                !repoService.checkCollaboratorWritePermission(ownerName, repoName, currentUserId) &&
                !repoIssueService.checkIssueOwner(currentUserId, ownerName, repoName, repoIssueId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        if(repoIssueService.checkIssueClosable(ownerName, repoName, repoIssueId)) {
            return new Return<>(ReturnCode.ISSUE_OPENED);
        }
        if( repoIssueService.reopenIssue(ownerName, repoName, repoIssueId)==1)
            return new Return<>(ReturnCode.OK,repoIssueId);
        else return new Return<>(ReturnCode.ISSUE_INTERNAL_ERROR);
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
