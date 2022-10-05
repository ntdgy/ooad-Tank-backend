package tank.ooad.fitgub.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tank.ooad.fitgub.entity.repo.RepoCollaborator;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
public class RepoSettingsController {
    private final RepoService repoService;
    public RepoSettingsController(RepoService repoService) {
        this.repoService = repoService;
    }

    // Repo Settings
    @RequireLogin
    @GetMapping("/api/repo/{reponame}/settings/collaborator")
    public Return<List<RepoCollaborator>> listRepoCollaborators(@PathVariable String reponame, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (!repoService.checkUserRepoOwner(userId, reponame)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        return new Return<>(ReturnCode.OK, repoService.getRepoCollaborators(userId, reponame));
    }

    @RequireLogin
    @PostMapping("/api/repo/{repoName}/settings/delete")
    public Return<Void> deleteRepo(@PathVariable String repoName, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        if (!repoService.checkUserRepoOwner(userId, repoName)) return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        repoService.dropRepo(userId, repoName);
        return Return.OK;
    }
}
