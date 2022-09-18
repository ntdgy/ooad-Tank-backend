package tank.ooad.fitgub.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.service.RepoService;
import tank.ooad.fitgub.utils.AttributeKeys;
import tank.ooad.fitgub.utils.Return;
import tank.ooad.fitgub.utils.ReturnCode;
import tank.ooad.fitgub.utils.permission.RequireLogin;

import javax.servlet.http.HttpSession;


@RestController
public class RepoController {

    private final RepoService repoService;

    public RepoController(RepoService repoService) {
        this.repoService = repoService;
    }

    @RequireLogin
    @PostMapping("/api/repo/create")
    public Return createRepo(@RequestBody Repo repo, HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);

        if (repoService.checkRepoDuplicate(repo, userId)) return new Return(ReturnCode.REPO_DUPLICATED);
        repoService.createRepo(repo, userId);
        return Return.OK;
    }

    @RequireLogin
    @GetMapping("/api/repo/list")
    public Return listMyRepo(HttpSession session) {
        int userId = (int) AttributeKeys.USER_ID.getValueNonNull(session);

        var lst = repoService.getUserRepos(userId);
        return new Return(ReturnCode.OK, lst);
    }
}
