package tank.ooad.fitgub.git;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class GitOperation {
    public record RepoStore(int userId, int repoId) {
    }

    private static final String REPO_STORE_PATH = "../repo-store";

    /***
     * Create RepoStore in disk.
     */
    public void createGitRepo(RepoStore repoStore) throws IOException {
        File repoPath = new File(REPO_STORE_PATH, String.format("%s/%s", repoStore.userId, repoStore.repoId));
        if (repoPath.exists())
            throw new RuntimeException("Repo " + repoPath.getAbsolutePath() + " shouldn't exists");
        var fileRepo = new FileRepository(repoPath);
        fileRepo.create(true);
        fileRepo.close();
    }

    public Repository getRepository(RepoStore repoStore) throws IOException {
        File repoPath = new File(REPO_STORE_PATH, String.format("%s/%s", repoStore.userId, repoStore.repoId));
        return new FileRepository(repoPath);
    }
}
