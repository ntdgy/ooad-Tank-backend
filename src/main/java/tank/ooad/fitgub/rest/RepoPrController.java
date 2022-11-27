package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.repo.Issue;
import tank.ooad.fitgub.entity.repo.IssueContent;
import tank.ooad.fitgub.entity.repo.PullRequest;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.exception.CustomException;
import tank.ooad.fitgub.git.GitOperation;
import tank.ooad.fitgub.service.MailService;
import tank.ooad.fitgub.service.RepoIssueService;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
public class RepoPrController {

    @Autowired
    private RepoIssueService repoIssueService;
    @Autowired
    private GitOperation gitOperation;
    @Autowired
    private RepoService repoService;

    @Autowired
    private MailService mailService;
    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/pull/create")
    public Return<Integer> createPullRequest(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            @RequestBody Issue issue,
            HttpSession session) {
        if (issue.pull == null) return new Return<>(ReturnCode.ILLEAL_ARGUMENTS);
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
        issue.pull.from = repoService.getRepo(issue.pull.from.getOwnerName(), issue.pull.from.getRepoName());
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        if (!repoService.checkRepoWritePermission(issue.pull.from, currentUserId)) throw new CustomException(ReturnCode.REPO_NO_PERMISSION);
        issue.pull.to = repoService.getRepo(ownerName, repoName);
        try {
            if (!gitOperation.checkBranchExistsInGitRepo(issue.pull.from, issue.pull.from_branch) || !gitOperation.checkBranchExistsInGitRepo(issue.pull.to, issue.pull.to_branch))
                throw new CustomException(ReturnCode.GIT_BRANCH_NON_EXIST);
        } catch (IOException e) {
            throw new CustomException(ReturnCode.SERVER_INTERNAL_ERROR);
        }
        repoIssueService.insertPullRequest(issueId, issue.pull);
        List<String> receivers = repoService.getRepoWatchers(repo.id);
        mailService.sendNewPrNotification(receivers, repo.name, issue.title);
        return new Return<>(ReturnCode.OK, issueIds.getValue());
    }

    @GetMapping("/api/repo/{ownerName}/{repoName}/pull")
    public Return<List<Issue>> listPullRequests(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            HttpSession session) {
        var repo = repoService.getRepo(ownerName, repoName);
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        if (!repoService.checkRepoReadPermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var issue = repoIssueService.listPulls(repo.id);
        return new Return<>(ReturnCode.OK, issue);
    }

    @GetMapping("/api/repo/{ownerName}/{repoName}/pull/{repoIssueId}")
    public Return<Issue> getPull(
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
        var issue = repoIssueService.getPull(repo.id, repoIssueId);
        return new Return<>(ReturnCode.OK, issue);
    }


    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/pull/{repoIssueId}/close")
    public Return<Void> closePull(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            @PathVariable int repoIssueId,
            HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        int issueId = repoIssueService.resolveIssue(ownerName, repoName, repoIssueId);
        if (issueId == -1){
            return new Return<>(ReturnCode.PULL_REQUEST_NOT_EXIST);
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
    @PostMapping("/api/repo/{ownerName}/{repoName}/pull/{repoIssueId}/reopen")
    public Return<Void> reopenPull(
            @PathVariable String ownerName,
            @PathVariable String repoName,
            @PathVariable int repoIssueId,
            HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        int issueId = repoIssueService.resolveIssue(ownerName, repoName, repoIssueId);
        if (issueId == -1){
            return new Return<>(ReturnCode.PULL_REQUEST_NOT_EXIST);
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
    @PostMapping("/api/repo/{ownerName}/{repoName}/pull/{repoIssueId}/addComment")
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
