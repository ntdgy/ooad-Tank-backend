package tank.ooad.fitgub.rest;

import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.entity.repo.RepoCollaborator;
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
}
