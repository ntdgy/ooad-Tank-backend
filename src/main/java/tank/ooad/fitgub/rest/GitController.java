package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import tank.ooad.fitgub.entity.git.GitRepo;
import tank.ooad.fitgub.entity.git.GitTreeEntry;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.git.GitOperation;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;

import javax.servlet.http.HttpSession;
import java.util.List;

@Component
@RestController
@Slf4j
public class GitController {


    private final GitOperation gitOperation;
    private final RepoService repoService;

    public GitController(GitOperation gitOperation, RepoService repoService) {
        this.gitOperation = gitOperation;
        this.repoService = repoService;
    }

    /**
     * /api/git/{username}/{reponame}/
     * Get Repo metadata
     *
     * @return GitCommitTree
     */
    @GetMapping("/api/git/{ownerName}/{repoName}")
    public Return<GitRepo> getCommitAndTree(@PathVariable String ownerName, @PathVariable String repoName,
                                            HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);

        // Resolve Repo
        Repo repo = repoService.getRepo(ownerName, repoName);
        if (repo == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);

        // checkPermission: require Read
        if (!repo.isPublic() && currentUserId != 0
                && !(repo.owner.id == currentUserId || repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        return new Return<>(ReturnCode.NOT_IMPLEMENTED);
//        return Return.OK;
    }

    @GetMapping("/api/git/{ownerName}/{repoName}/tree/{ref}/{*path}")
    public Return<List<GitTreeEntry>> getTree(@PathVariable String ownerName,
                                              @PathVariable String repoName,
                                              @PathVariable String ref,
                                              @PathVariable String path,
                                              HttpSession session) {
        log.info(ref);
        log.info(path);
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repository = repoService.getRepo(ownerName, repoName);
        if (repository == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repository.isPublic() && currentUserId != 0
                && !(repository.owner.id == currentUserId || repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        try {
            var fileList = gitOperation.readGitTree(repository, ref, path);
            return new Return<>(ReturnCode.OK, fileList);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
//            return new Return<>(ReturnCode.GitAPIError);
        }
    }

    @GetMapping("/api/git/{ownerName}/{repoName}/blob/{ref}/{path}")
    public Return<String> getBlob(@PathVariable String ownerName, @PathVariable String repoName,
                                  @PathVariable String ref, @RequestParam String path, HttpSession session) {
        return new Return<>(ReturnCode.NOT_IMPLEMENTED);
    }


}
