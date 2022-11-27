package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tank.ooad.fitgub.entity.git.GitBlob;
import tank.ooad.fitgub.entity.git.GitPerson;
import tank.ooad.fitgub.entity.git.GitRepo;
import tank.ooad.fitgub.entity.git.GitTreeEntry;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.git.GitOperation;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.Utils;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
                                            HttpSession session) throws IOException {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);

        // Resolve Repo
        Repo repo = repoService.getRepo(ownerName, repoName);

        // checkPermission: require Read
        if (!repo.isPublic() && currentUserId != 0
                && !(repo.owner.id == currentUserId || repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        return new Return<>(ReturnCode.OK, gitOperation.getGitRepo(repo));
//        return Return.OK;
    }

    @GetMapping("/api/git/{ownerName}/{repoName}/tree/{ref}/{*path}")
    public Return<List<GitTreeEntry>> getTree(@PathVariable String ownerName,
                                              @PathVariable String repoName,
                                              @PathVariable String ref,
                                              @PathVariable String path,
                                              HttpSession session) {
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

    @GetMapping("/api/git/{ownerName}/{repoName}/blob/{ref}/{*path}")
    public Return<GitBlob> getBlob(@PathVariable String ownerName,
                                   @PathVariable String repoName,
                                   @PathVariable String ref,
                                   @PathVariable String path,
                                   HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repository = repoService.getRepo(ownerName, repoName);
        if (repository == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repository.isPublic() && currentUserId != 0
                && !(repository.owner.id == currentUserId || repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        try {
            var loader = gitOperation.getGitBlobLoader(repository, ref, path);
            if (loader == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
            if (Utils.isBinaryFile(path) || loader.getSize() > 1024 * 1024L)
                return new Return<>(ReturnCode.OK, new GitBlob(false, loader.getSize()));
            // too large file or binary file
            var blob = new GitBlob(true, loader.getSize());
            blob.content = new String(loader.openStream().readAllBytes());
            return new Return<>(ReturnCode.OK, blob);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/api/git/{ownerName}/{repoName}/raw/{ref}/{*path}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getRaw(@PathVariable String ownerName,
                                                      @PathVariable String repoName,
                                                      @PathVariable String ref,
                                                      @PathVariable String path,
                                                      HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repository = repoService.getRepo(ownerName, repoName);
        if (repository == null) return ResponseEntity.notFound().build();
        if (!repository.isPublic() && currentUserId != 0
                && !(repository.owner.id == currentUserId || repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return ResponseEntity.notFound().build();
        }
        try {
            var loader = gitOperation.getGitBlobLoader(repository, ref, path);
            try (var istream = loader.openStream()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new InputStreamResource(istream));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/api/git/{ownerName}/{repoName}/upload")
    @RequireLogin
    public Return<String> uploadFile(@PathVariable String ownerName,
                                     @PathVariable String repoName,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam("branch") String branch,
                                     @RequestParam("committerName") String committerName,
                                     @RequestParam("committerEmail") String committerEmail,
                                     @RequestParam("message") String message,
                                     @RequestParam("path") String path,
                                     HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);
        Repo repository = repoService.getRepo(ownerName, repoName);
        if (repository == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);
        if (!repoService.checkRepoWritePermission(repository,currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        try {
            GitPerson committer = new GitPerson(committerName, committerEmail);
            Map<String, byte[]> contents = new HashMap<>();
            contents.put(path + file.getOriginalFilename(), file.getBytes());
            if (Objects.equals(message, "")){
                message = "Add file %s.".formatted(file.getName());
            }
            gitOperation.commit(repository,branch, committer, message, contents);
            return new Return<>(ReturnCode.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
