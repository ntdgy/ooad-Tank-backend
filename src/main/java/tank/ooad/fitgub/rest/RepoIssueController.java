package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.repo.Issue;
import tank.ooad.fitgub.entity.repo.IssueContent;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.service.MailService;
import tank.ooad.fitgub.service.RepoIssueService;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@Slf4j
public class RepoIssueController {

    @Autowired
    private RepoIssueService repoIssueService;
    @Autowired
    private RepoService repoService;

    @Autowired
    private MailService mailService;

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/issue/create")
    public Return<Integer> createIssue(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            @RequestBody Issue issue,
            HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (issue.tag == null) issue.tag = List.of();
        String tag = String.join(",", issue.tag);
        Repo repo = repoService.getRepo(ownerName, repoName);
        if (!repoService.checkRepoReadPermission(repo, userId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var issueIds = repoIssueService.createIssue(repo.id, issue.title, userId, tag);
        int issueId = issueIds.getKey();
        for (var content : issue.contents) {
            repoIssueService.insertIssueContent(issueId, userId, content);
        }
        List<String> receivers = repoService.getRepoWatchers(repo.id);
        System.out.println(receivers);
        mailService.sendNewIssueNotification(receivers, repo.name, issue.title);
        return new Return<>(ReturnCode.OK, issueIds.getValue());
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
        if (!repoService.checkRepoReadPermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var issue = repoIssueService.getIssue(repo.id, repoIssueId);
        return new Return<>(ReturnCode.OK, issue);
    }

    @GetMapping("/api/repo/{ownerName}/{repoName}/issue")
    public Return<List<Issue>> listIssues(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            HttpSession session) {
        var repo = repoService.getRepo(ownerName, repoName);
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        if (!repoService.checkRepoReadPermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var issue = repoIssueService.listIssues(repo.id);
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
        if (issueId == -1) {
            return new Return<>(ReturnCode.ISSUE_NOT_EXIST);
        }
        var repo = repoService.getRepo(ownerName, repoName);
        if (!(repoService.checkRepoWritePermission(repo, currentUserId) || repoIssueService.checkIssueOwner(currentUserId, issueId))) {
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
        if (issueId == -1) {
            return new Return<>(ReturnCode.ISSUE_NOT_EXIST);
        }
        var repo = repoService.getRepo(ownerName, repoName);
        if (!repoService.checkRepoWritePermission(repo, currentUserId) || !repoIssueService.checkIssueOwner(currentUserId, issueId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        if (repoIssueService.checkIssueClosable(issueId)) {
            return new Return<>(ReturnCode.ISSUE_OPENED);
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
        var repo = repoService.getRepo(ownerName, repoName);
        var issueId = repoIssueService.resolveIssue(repo.id, repoIssueId);
        if (!repoService.checkRepoReadPermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        int contentId = repoIssueService.insertIssueContent(issueId, currentUserId, issueContent);
        return new Return<>(ReturnCode.OK, contentId);
    }


}
