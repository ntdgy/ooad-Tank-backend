package tank.ooad.fitgub.exception;

public class GitRepoNonExistException extends Exception {
    String repoName;


    public GitRepoNonExistException(String repoName) {
        this.repoName = repoName;
    }


}

