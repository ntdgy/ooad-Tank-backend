package tank.ooad.fitgub.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.repo.RepoCollaborator;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.git.GitOperation;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.service.UserService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
public class RepoSettingsController {
    private final RepoService repoService;
    private final UserService userService;
    private final GitOperation gitController;

    public RepoSettingsController(RepoService repoService, UserService userService, GitOperation gitController) {
        this.repoService = repoService;
        this.userService = userService;
        this.gitController = gitController;
    }

    // Repo Settings
    @RequireLogin
    @GetMapping("/api/repo/{ownerName}/{repoName}/settings/collaborator")
    public Return<List<RepoCollaborator>> listRepoCollaborators(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (!repoService.checkRepoOwnerPermission(userId, ownerName, repoName)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        var cols = repoService.getRepoCollaborators(userId, repoName);
        return new Return<>(ReturnCode.OK, cols);
    }

    /**
     * Add collaborator with email or username
     */
    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/settings/collaborator")
    public Return<List<RepoCollaborator>> addOrAlterCollaborators(@PathVariable String ownerName, @PathVariable String repoName,
                                                                  @RequestParam(required = false, name = "delete") Optional<Integer> delete,
                                                                  @RequestBody RepoCollaborator collaborator, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValue(session);
        if (!repoService.checkRepoOwnerPermission(userId, ownerName, repoName)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        // TODO: 判断user哪些参数应该唯一
        User invited = userService.findUserByName(collaborator.user.name, collaborator.user.email);
        if (invited == null || invited.id == userId)
            return new Return<>(ReturnCode.USER_NOTFOUND);
        if (delete.isPresent()) {
            repoService.removeRepoCollaborator(userId, repoName, invited.id);
        } else {
            int permission = RepoCollaborator.COLLABORATOR_READ;
            if (collaborator.canWrite) permission |= RepoCollaborator.COLLABORATOR_WRITE;
            repoService.addRepoCollaborator(userId, repoName, invited.id, permission);
        }
        return new Return<>(ReturnCode.OK, repoService.getRepoCollaborators(userId, repoName));
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/settings/delete")
    public Return<Void> deleteRepo(@PathVariable String ownerName, @PathVariable String repoName, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        if (!repoService.checkRepoOwnerPermission(userId, ownerName, repoName))
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        var repo = repoService.getRepo(userId, repoName);
        try {
            gitController.deleteGitRepo(repo);
            repoService.dropRepo(userId, repoName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Return.OK;
    }

    @RequireLogin
    @PostMapping("/api/repo/{ownerName}/{repoName}/settings/default_branch")
    public Return<Void> setRepoDefaultBranch(@PathVariable String ownerName, @PathVariable String repoName, @RequestBody String postBody, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);
        if (!repoService.checkRepoOwnerPermission(userId, ownerName, repoName))
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        var repo = repoService.getRepo(userId, repoName);
        try {
            JsonNode json = new ObjectMapper().readTree(postBody);
            String name = json.get("name").asText();
            if(!gitController.changeDefaultBranch(repo, name)) {
                return new Return<>(ReturnCode.GIT_BRANCH_NON_EXIST);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Return.OK;
    }
}
