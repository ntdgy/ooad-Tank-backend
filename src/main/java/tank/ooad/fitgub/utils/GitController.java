package tank.ooad.fitgub.utils;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import tank.ooad.fitgub.entity.User;

import java.io.File;

import static tank.ooad.fitgub.utils.ReturnCode.GitAPIError;


public class GitController {

    public static void main(String[] args) {
        var user = new User();
        user.id = 0;
        user.name = "frank";
        user.password = "123456";
        user.email = "12011211@mail.sustech.edu.cn";
        var repo = "test2";
        System.out.println(initRepo(user, repo));
//        cloneRepo(user, repo,"https://github.com/ntdgy/github-actions.git");
    }

    public static Return initRepo(User user, String repoName)  {
        try {
            boolean exist = checkExist(user, repoName);
            if (exist) {
                return new Return(ReturnCode.GitRepoExist);
            }
            String defaultPath = "repo/" + user.id + "/" + repoName;
            Git git = Git.init().setDirectory(new File(defaultPath)).call();
        }catch (GitAPIException e){
            return new Return(GitAPIError,e.getMessage());
        }
        return new Return(ReturnCode.OK);
    }

    public static Return cloneRepo(User user, String repoName, String url)  {
        try {
            boolean exist = checkExist(user, repoName);
            if (exist) {
                return new Return(ReturnCode.GitRepoExist);
            }
            String defaultPath = "repo/" + user.id + "/" + repoName;
            Git git = Git.cloneRepository().setURI(url).setDirectory(new File(defaultPath)).call();
        }catch (GitAPIException e){
            return new Return(GitAPIError,e.getMessage());
        }
        return new Return(ReturnCode.OK);
    }

    public static boolean checkExist(User user, String repoName){
        String defaultPath = "repo/" + user.id + "/" + repoName;
        File file = new File(defaultPath);
        return file.exists();
    }
}
