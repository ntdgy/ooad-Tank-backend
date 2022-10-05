package tank.ooad.fitgub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RepoIssueService {
    @Autowired
    private JdbcTemplate jdbcTemplate;



}
