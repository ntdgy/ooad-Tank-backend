package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.repo.Issue;
import tank.ooad.fitgub.entity.repo.IssueContent;
import tank.ooad.fitgub.service.RepoIssueService;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
public class RepoIssueController {

    @Autowired
    private RepoIssueService repoIssueService;
    @Autowired
    private RepoService repoService;

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/issue/create")
    public Return<Integer> createIssue(
            @PathVariable String ownerName,
            @PathVariable String repoName,
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
        String tag = sb.toString();

        int repoId = repoService.resolveRepo(ownerName, repoName);
        int issueId = repoIssueService.createIssue(repoId, issue.title, userId, tag);
        for (var content : issue.contents) {
            repoIssueService.insertIssueContent(issueId, userId, content);
        }
        return new Return<>(ReturnCode.OK, issueId);
    }

    @GetMapping("/api/repo/{ownerName}/{repoName}/issue/{repoIssueId}")
    public Return<Issue> getIssue(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            @PathVariable int repoIssueId,
            HttpSession session) {
        // check private repo
        var repo = repoService.getRepo(ownerName, repoName);
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        if (!repo.isPublic() && (currentUserId == 0 || !repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var issue = repoIssueService.getIssue(ownerName, repoName, repoIssueId);
        repoIssueService.loadContents(issue);
        return new Return<>(ReturnCode.OK, issue);
    }

    @GetMapping("/api/repo/{ownerName}/{repoName}/issue")
    public Return<List<Issue>> listIssues(
            @PathVariable String ownerName,
            @PathVariable String repoName) {
        var issue = repoIssueService.listIssues(ownerName, repoName);
        return new Return<>(ReturnCode.OK, issue);
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/issue/{repoIssueId}/close")
    public Return<Void> closeIssue(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            @PathVariable int repoIssueId,
            HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        int issueId = repoIssueService.resolveIssue(ownerName, repoName, repoIssueId);
        if (!repoService.checkUserRepoOwner(currentUserId, ownerName, repoName) && !repoService.checkCollaboratorWritePermission(ownerName, repoName, currentUserId) && !repoIssueService.checkIssueOwner(currentUserId, issueId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        if (!repoIssueService.checkIssueClosable(issueId)) {
            return new Return<>(ReturnCode.ISSUE_CLOSED);
        }
        repoIssueService.closeIssue(issueId);
        return Return.OK;
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/issue/{repoIssueId}/reopen")
    public Return<Void> reopenIssue(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            @PathVariable int repoIssueId,
            HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        int issueId = repoIssueService.resolveIssue(ownerName, repoName, repoIssueId);
        if (!repoService.checkUserRepoOwner(currentUserId, ownerName, repoName) && !repoService.checkCollaboratorWritePermission(ownerName, repoName, currentUserId) && !repoIssueService.checkIssueOwner(currentUserId, issueId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        if (!repoIssueService.checkIssueClosable(issueId)) {
            return new Return<>(ReturnCode.ISSUE_CLOSED);
        }
        repoIssueService.reopenIssue(issueId);
        return Return.OK;
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/issue/{repoIssueId}/addComment")
    public Return<Integer> addComment(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            @PathVariable int repoIssueId,
            @RequestBody IssueContent issueContent,
            HttpSession httpsession) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(httpsession);
        var issue = repoIssueService.getIssue(ownerName, repoName, repoIssueId);
        int contentId = repoIssueService.insertIssueContent(issue.id,currentUserId,issueContent);
        return new Return<>(ReturnCode.OK,contentId);
    }


}
