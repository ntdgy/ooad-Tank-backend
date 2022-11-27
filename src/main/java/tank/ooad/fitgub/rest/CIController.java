package tank.ooad.fitgub.rest;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.pqc.crypto.rainbow.Layer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tank.ooad.fitgub.entity.ci.CiWork;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.service.CIService;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

import static tank.ooad.fitgub.utils.ReturnCode.OK;

@RestController
@Slf4j
public class CIController {
    @Autowired
    private CIService ciService;

    @Autowired
    private RepoService repoService;

    @PostMapping("/api/repo/{userName}/{repoName}/ci/run")
    @RequireLogin
    public Return<List<String>> runCI(@PathVariable String userName,
                                      @PathVariable String repoName,
                                      @RequestParam String ciName,
                                      @RequestParam("file") MultipartFile file,
                                      HttpSession httpSession) throws IOException {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(httpSession);
        log.error("currentUserId: " + currentUserId);
        Repo repo = repoService.getRepo(userName, repoName);
        if (!repoService.checkRepoWritePermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        return new Return<>(OK, ciService.runCI(repo.id, currentUserId, ciName,file.getInputStream()));
    }

    @GetMapping("/api/repo/{userName}/{repoName}/ci/list")
    @RequireLogin
    public Return<List<CiWork>> getCIList(@PathVariable String userName,
                                          @PathVariable String repoName,
                                          HttpSession httpSession) throws IOException {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(httpSession);
        log.error("currentUserId: " + currentUserId);
        Repo repo = repoService.getRepo(userName, repoName);
        if (!repoService.checkRepoWritePermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        return new Return<>(OK, ciService.getCIList(repo.id));
    }

    @GetMapping("/api/repo/{userName}/{repoName}/ci/{id}/log")
    @RequireLogin
    public Return<String> getCIList(@PathVariable String userName,
                                    @PathVariable String repoName,
                                    @PathVariable int id,
                                    HttpSession httpSession) throws IOException {
        int currentUserId = (int) AttributeKeys.USER_ID.getValue(httpSession);
        log.error("currentUserId: " + currentUserId);
        Repo repo = repoService.getRepo(userName, repoName);
        if (!repoService.checkRepoReadPermission(repo, currentUserId)) {
            return new Return<>(ReturnCode.GIT_REPO_NO_PERMISSION);
        }
        return new Return<>(OK, ciService.getCIOutput(id));
    }



}
