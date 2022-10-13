package tank.ooad.fitgub.git;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.git.GitCommit;
import tank.ooad.fitgub.entity.git.GitPerson;
import tank.ooad.fitgub.entity.git.GitRepo;
import tank.ooad.fitgub.entity.git.GitTreeEntry;
import tank.ooad.fitgub.entity.repo.Repo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitOperation {

    private static final String REPO_STORE_PATH = "../repo-store";

    /***
     * Create RepoStore in disk.
     */
    public void createGitRepo(Repo repo) throws IOException {
        File repoPath = new File(REPO_STORE_PATH, String.format("%s/%s", repo.owner.id, repo.id));
        if (repoPath.exists())
            throw new RuntimeException("Repo " + repoPath.getAbsolutePath() + " shouldn't exists");
        var fileRepo = new FileRepository(repoPath);
        fileRepo.create(true);
        fileRepo.close();
    }

    public void deleteGitRepo(Repo repo) throws IOException {
        File repoPath = new File(REPO_STORE_PATH, String.format("%s/%s", repo.owner.id, repo.id));
        if (!repoPath.exists())
            throw new RuntimeException("Repo " + repoPath.getAbsolutePath() + " shouldn't exists");
        FileUtils.deleteDirectory(repoPath);
    }

    public Repository getRepository(Repo repo) throws IOException {
        File repoPath = new File(REPO_STORE_PATH, String.format("%s/%s", repo.owner.id, repo.id));
        return new FileRepository(repoPath);
    }

    public GitRepo getGitRepo(Repo repo) throws IOException {
        GitRepo gitRepo = new GitRepo();
        Repository repository = getRepository(repo);
        gitRepo.default_branch = repository.getBranch();
        gitRepo.branches = repository.getRefDatabase().getRefsByPrefix("refs/heads/").stream().map(Ref::getName).toList();
        gitRepo.tags = repository.getRefDatabase().getRefsByPrefix("refs/tags/").stream().map(Ref::getName).toList();
        var HEAD = repository.exactRef("HEAD");
        if (HEAD.getObjectId() != null) {
            var commit = repository.parseCommit(HEAD.getObjectId());
            gitRepo.head = new GitCommit();
            gitRepo.head.commit_hash = commit.getName();
            gitRepo.head.commit_message = commit.getFullMessage();
            gitRepo.head.commit_time = commit.getCommitterIdent().getWhen().getTime();
            gitRepo.head.author = new GitPerson();
            var author = commit.getAuthorIdent();
            gitRepo.head.author.name = author.getName();
            gitRepo.head.author.email = author.getEmailAddress();
            gitRepo.head.committer = new GitPerson();
            var committer = commit.getCommitterIdent();
            gitRepo.head.committer.name = committer.getName();
            gitRepo.head.committer.email = committer.getEmailAddress();
        }
        return gitRepo;
    }

    public List<GitTreeEntry> readGitTree(Repo repo,
                                          String Ref,
                                          String path) throws IOException {
        Repository repository = getRepository(repo);
        var head = repository.resolve(Ref);
        if (head == null) return List.of();
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(head);
        RevTree tree = commit.getTree();
        List<GitTreeEntry> files = new ArrayList<>();
        var prefix = path.split("/");
        if (prefix.length == 0) prefix = new String[]{""};
        int len = 1;
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(false);
        a:
        while (len < prefix.length) {
            len++;
            while (treeWalk.next()) {
                if (treeWalk.isSubtree() && treeWalk.getNameString().equals(prefix[len - 1])) {
                    treeWalk.enterSubtree();
                    continue a;
                }
            }
        }
        while (treeWalk.next() && treeWalk.getDepth() == len - 1) {
            if (treeWalk.isSubtree()) {
                files.add(new GitTreeEntry(treeWalk.getNameString() + "/"));
            } else
                files.add(new GitTreeEntry(treeWalk.getNameString()));
        }
        treeWalk.close();
        return files;
    }

    public String readGitBlob(Repo repo, String ref, String path) throws IOException {
        Repository repository = getRepository(repo);
        var head = repository.resolve(ref);
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(head);
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(false);
        var prefix = path.split("/");
        int len = 1;
        a:
        while (len < prefix.length - 1) {
            len++;
            while (treeWalk.next()) {
                if (treeWalk.isSubtree() && treeWalk.getNameString().equals(prefix[len - 1])) {
                    treeWalk.enterSubtree();
                    continue a;
                }
            }
        }
        while (treeWalk.next() && treeWalk.getDepth() == len - 1) {
            if (treeWalk.getNameString().equals(prefix[len])) {
                var blob = treeWalk.getObjectId(0);
                var loader = repository.open(blob);
                return new String(loader.getBytes());
            }
        }
        return null;
    }
}
