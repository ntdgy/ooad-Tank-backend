package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.entity.repo.RepoCollaborator;
import tank.ooad.fitgub.entity.repo.RepoMetaData;
import tank.ooad.fitgub.git.GitOperation;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
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

        if (repoService.checkRepoDuplicate(repo, userId)) return new Return<>(ReturnCode.REPO_DUPLICATED);
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
    public Return<List<Repo>> listMyRepo(HttpSession session, @RequestParam Optional<String> permission) {
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        var lst = repoService.getUserRepos(userId);
        return new Return<>(ReturnCode.OK, lst);
    }

    @GetMapping("/api/repo/list_pub/{username}")
    public Return<List<Repo>> listUserPublicRepo(@PathVariable String username) {
        var lst = repoService.getUserPublicRepos(username);
        return new Return<>(ReturnCode.OK, lst);
    }

    @RequireLogin
    @GetMapping("/api/repo/{ownerName}/{repoName}/setPublic")
    public Return<Void> setPublic(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        System.out.println("setPublic");
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        var repo = repoService.getRepo(ownerName, repoName);
        if (repo == null) return new Return<>(ReturnCode.REPO_NON_EXIST);
        if (repo.owner.id != userId) return new Return<>(ReturnCode.REPO_NO_PERMISSION);
        if (repo.isPublic()) return new Return<>(ReturnCode.REPO_ALREADY_PUBLIC);
        repoService.setPublic(repo);
        return Return.OK;
    }

    @RequireLogin
    @GetMapping("/api/repo/{ownerName}/{repoName}/setPrivate")
    public Return<Void> setPrivate(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        System.out.println("setPrivate");
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        var repo = repoService.getRepo(ownerName, repoName);
        if (repo == null) return new Return<>(ReturnCode.REPO_NON_EXIST);
        if (repo.owner.id != userId) return new Return<>(ReturnCode.REPO_NO_PERMISSION);
        if (!repo.isPublic()) return new Return<>(ReturnCode.REPO_ALREADY_PRIVATE);
        repoService.setPrivate(repo);
        return Return.OK;
    }

    @GetMapping("/api/repo/{ownerName}/{repoName}/metaData")
    public Return<RepoMetaData> getRepoMetaData(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repository = repoService.getRepo(ownerName, repoName);
        if (repository == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repository.isPublic() && currentUserId != 0
                && !(repository.owner.id == currentUserId || repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var repoMetaData = repoService.getRepoMetaData(repository);
        repoMetaData.owner = repository.owner;
        repoMetaData.name = repository.name;
        var forkedData = repoService.getForkedRepoNames(repository);
        if(!forkedData.isEmpty()) {
            repoMetaData.fork_from_name = forkedData.get(0);
            repoMetaData.fork_from_owner = forkedData.get(1);
        }
        return new Return<>(ReturnCode.OK, repoMetaData);
    }

    @RequireLogin
    @GetMapping("/api/repo/{ownerName}/{repoName}/action/star")
    public Return<Integer> starRepo(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repository = repoService.getRepo(ownerName, repoName);
        if (repository == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repository.isPublic() && currentUserId != 0
                && !(repository.owner.id == currentUserId || repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var stars = repoService.starRepo(currentUserId,repository.id);
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
        var stars = repoService.unstarRepo(currentUserId,repository.id);
        if (stars == -1) return new Return<>(ReturnCode.REPO_ALREADY_UNSTARRED);
        return new Return<>(ReturnCode.OK, stars);
    }



}
