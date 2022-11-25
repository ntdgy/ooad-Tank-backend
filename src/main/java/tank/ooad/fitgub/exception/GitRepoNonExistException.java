package tank.ooad.fitgub.exception;

import java.io.UncheckedIOException;

public class GitRepoNonExistException extends RuntimeException {
    public final String ownerName;
    public final String repoName;


    public GitRepoNonExistException(String ownerName, String repoName) {
        this.ownerName = ownerName;
        this.repoName = repoName;
    }


}

