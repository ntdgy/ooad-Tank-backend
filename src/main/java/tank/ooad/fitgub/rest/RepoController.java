package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.git.GitRepo;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.entity.repo.RepoMetaData;
import tank.ooad.fitgub.exception.GitRepoNonExistException;
import tank.ooad.fitgub.git.GitOperation;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import javax.xml.transform.OutputKeys;
import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
public class RepoController {

    private final RepoService repoService;
    private final GitOperation gitOperation;

    public RepoController(RepoService repoService, GitOperation gitOperation) {
        this.repoService = repoService;
        this.gitOperation = gitOperation;
    }

    @RequireLogin
    @PostMapping("/api/repo/create")
    public Return<Void> createRepo(@RequestBody Repo repo, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);

        if (repoService.checkRepoDuplicate(repo.name, userId)) return new Return<>(ReturnCode.REPO_DUPLICATED);
        int repoId = repoService.createRepo(repo, userId);
        var createdRepo = repoService.getRepo(userId, repo.name);
        try {
            gitOperation.createGitRepo(createdRepo);
        } catch (Exception e) {
            repoService.dropRepo(repoId);
            throw new RuntimeException(e);
        }
        return Return.OK;
    }

    @RequireLogin
    @GetMapping("/api/repo/list_self")
    public Return<List<Repo>> listMyRepo(HttpSession session) throws GitRepoNonExistException {
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        return new Return<>(ReturnCode.OK, repoService.getUserRepos(userId));
    }

    @GetMapping("/api/repo/list_pub/{username}")
    public Return<List<Repo>> listUserPublicRepo(@PathVariable String username, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        var lst = repoService.getUserPublicRepos(username, currentUserId);
        return new Return<>(ReturnCode.OK, lst);
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/setPublic")
    public Return<Void> setPublic(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        System.out.println("setPublic");
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        var repoId = repoService.resolveRepo(ownerName, repoName);
        if (!repoService.checkRepoOwnerPermission(userId, ownerName, repoName)) {
            return new Return<>(ReturnCode.REPO_NO_PERMISSION);
        }
        repoService.setPublic(repoId);
        return Return.OK;
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/setPrivate")
    public Return<Void> setPrivate(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        System.out.println("setPrivate");
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        var repoId = repoService.resolveRepo(ownerName, repoName);
        if (!repoService.checkRepoOwnerPermission(userId, ownerName, repoName)) {
            return new Return<>(ReturnCode.REPO_NO_PERMISSION);
        }
        repoService.setPrivate(repoId);
        return Return.OK;
    }

    @GetMapping("/api/repo/{ownerName}/{repoName}")
    public Return<Repo> getRepo(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repo = repoService.getRepo(ownerName, repoName);
        if (!repoService.checkRepoReadPermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        repoService.fillStarAndWatch(repo, currentUserId);
        return new Return<>(ReturnCode.OK, repo);
    }


    @GetMapping("/api/repo/{ownerName}/{repoName}/metaData")
    public Return<RepoMetaData> getRepoMetaData(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repo = repoService.getRepo(ownerName, repoName);
        if (repo == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repoService.checkRepoReadPermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var repoMetaData = repoService.getRepoMetaData(repo);
        return new Return<>(ReturnCode.OK, repoMetaData);
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/updateMetaData")
    public Return<Boolean> updateRepoMetaData(@RequestBody RepoMetaData repoMetaData, @PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        Repo repo = repoService.getRepo(ownerName, repoName);
        if (repo == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repoService.checkRepoWritePermission(repo, userId)) {
            return new Return<>(ReturnCode.REPO_NO_PERMISSION);
        }
        return new Return<>(ReturnCode.OK, repoService.updateRepoMetaData(repo, repoMetaData));
    }

    @RequireLogin
    @GetMapping("/api/repo/{ownerName}/{repoName}/action/star")
    public Return<Integer> starRepo(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repo = repoService.getRepo(ownerName, repoName);
        if (repo == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repoService.checkRepoReadPermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var stars = repoService.starRepo(currentUserId, repo.id);
        if (stars == -1) return new Return<>(ReturnCode.REPO_ALREADY_STARRED);
        return new Return<>(ReturnCode.OK, stars);
    }

    @RequireLogin
    @GetMapping("/api/repo/{ownerName}/{repoName}/action/unStar")
    public Return<Integer> unstarRepo(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repository = repoService.getRepo(ownerName, repoName);
        if (repository == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repository.isPublic() && currentUserId != 0
                && !(repository.owner.id == currentUserId || repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var stars = repoService.unstarRepo(currentUserId, repository.id);
        if (stars == -1) return new Return<>(ReturnCode.REPO_ALREADY_UNSTARRED);
        return new Return<>(ReturnCode.OK, stars);
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/fork")
    public Return<Void> forkGitRepo(@PathVariable String ownerName, @PathVariable String repoName, @RequestBody Repo repo, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo originRepo = repoService.getRepo(ownerName, repoName);
        if (originRepo == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!originRepo.isPublic() && currentUserId != 0
                && !(originRepo.owner.id == currentUserId || repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        if (repoService.checkRepoDuplicate(repo.name, currentUserId))
            return new Return<>(ReturnCode.GitRepoExist);
        var forkedRepo = repoService.getRepo(repoService.createRepo(repo, currentUserId));
        var metaData = repoService.getRepoMetaData(forkedRepo);
        metaData.forked_from_id = originRepo.id;
        repoService.updateRepoMetaData(forkedRepo, metaData);
        try {
            gitOperation.forkGitRepo(originRepo, forkedRepo);
        } catch (Exception e) {
            repoService.dropRepo(forkedRepo.id);
            throw new RuntimeException(e);
        }
        return Return.OK;
    }

    @GetMapping("/api/repo/{ownerName}/{repoName}/actions")
    public Return<List<Boolean>> getRepoActions(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repo = repoService.getRepo(ownerName, repoName);
        if (repo == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repoService.checkRepoReadPermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var repoActions = repoService.getUserRepoAction(currentUserId, repo.id);
        return new Return<>(ReturnCode.OK, repoActions);
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/pages")
    public Return<Boolean> setPagesUp(@PathVariable String ownerName, @PathVariable String repoName, @RequestParam boolean status, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repo = repoService.getRepo(ownerName, repoName);
        if (repo == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repoService.checkRepoWritePermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var metaData = repoService.getRepoMetaData(repo);
        if (metaData.hasPage == status) {
            return new Return<>(ReturnCode.REPO_PAGE_ALREADY_EXIST);
        }
        repoService.setRepoPageStatus(repo, status);
        return new Return<>(ReturnCode.OK, true);
    }

    @GetMapping("/api/repo/suggest")
    public Return<Repo> suggestRepo() {
        return new Return<>(ReturnCode.OK, repoService.getRandomRepo());
    }

    @PostMapping("/api/repo/search")
    public Return<List<Repo>> searchRepo(@RequestParam String keyword) {
        return new Return<>(ReturnCode.OK, repoService.searchRepo(keyword));
    }


}
