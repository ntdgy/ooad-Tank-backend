package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tank.ooad.fitgub.entity.git.GitCommitTree;
import tank.ooad.fitgub.entity.git.GitTreeEntry;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.git.GitOperation;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
     * /api/git/{username}/{reponame}/commit_tree -> HEAD
     * /api/git/{username}/{reponame}/commit_tree?resolve=?
     *      resolve -> tag/commit_hash/branch
     *
     * @return GitCommitTree
     */
    @GetMapping("/api/git/{ownerName}/{repoName}/commit_tree")
    public Return<GitCommitTree> getCommitAndTree(@PathVariable String ownerName, @PathVariable String repoName,
                                                  @RequestParam Optional<String> resolve,
                                                  HttpSession session) {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(session);

        // Resolve Repo
        Repo repo = repoService.getRepo(ownerName, repoName);
        if (repo == null) return new Return<>(ReturnCode.GIT_REPO_NON_EXIST);

        // checkPermission: require Read
        if (!repo.isPublic() && (currentUserId == 0 || !repoService.checkCollaboratorReadPermission(ownerName, repoName, currentUserId))) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }

        try (Repository repository = gitOperation.getRepository(repo)) {
            ObjectId targetCommit = null;
            if (resolve.isPresent()) targetCommit = repository.resolve(resolve.get());
            else targetCommit = repository.resolve("HEAD");

            GitCommitTree commitTree = new GitCommitTree();
            commitTree.commit_hash = targetCommit.name();

            RevWalk revWalk = new RevWalk(repository);

            // get commit object
            var commitObj = revWalk.parseCommit(targetCommit);
            // set committer and author
            var committer = commitObj.getCommitterIdent();
            commitTree.committer = committer.getName();
            commitTree.committer_email = committer.getEmailAddress();
            var author = commitObj.getAuthorIdent();
            commitTree.author = author.getName();
            commitTree.author_email = author.getEmailAddress();
            // set commit message
            commitTree.commit_message = commitObj.getShortMessage();

            var treeId = commitObj.getTree().getId();
            revWalk.close();

            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.reset(treeId);

            List<GitTreeEntry> gitTree = new ArrayList<>();
            while (treeWalk.next()) {
                String name = treeWalk.getNameString();
                String hash = treeWalk.getObjectId(0).name();
                gitTree.add(new GitTreeEntry(name, hash, treeWalk.isSubtree()));
            }
            treeWalk.close();
            commitTree.tree = gitTree;
            return new Return<>(ReturnCode.OK, commitTree);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        return Return.OK;
    }

}
