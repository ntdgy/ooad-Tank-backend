package tank.ooad.fitgub.service;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.entity.user.VerificationCode;
import tank.ooad.fitgub.utils.Crypto;

import java.util.HashMap;
import java.util.List;

@Component
public class UserService {
    private final JdbcTemplate jdbcTemplate;

    private final MailService mailService;

    private final HashMap<Integer, VerificationCode> verificationCodeHashMap;

    public UserService(JdbcTemplate template, MailService mailService) {
        this.jdbcTemplate = template;
        this.mailService = mailService;
        this.verificationCodeHashMap = new HashMap<>();
    }

    public boolean checkExist(String username, String email) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from users where name = ? or email=?", Integer.class, username, email);
        return count != null && count != 0;
    }

    public boolean checkExist(String email) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from users where email=?", Integer.class, email);
        return count != null && count != 0;
    }

    public boolean checkExist(int githubId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from users where github_id=?", Integer.class, githubId);
        return count != null && count != 0;
    }

    /**
     * Validate a user with given (username or email) and raw password, and get its userId if valid
     *
     * @return 0 if invalid, otherwise a valid user id
     */
    public int validateUser(String username, String email, String rawPassword) {
        try {
            if (username != null) {
                Integer id = jdbcTemplate.queryForObject("select id from users where name = ? and password = ?;", Integer.class, username, Crypto.hashPassword(rawPassword));
                if (id != null) return id;
            }
            if (email != null) {
                Integer id = jdbcTemplate.queryForObject("select id from users where email = ? and password = ?;", Integer.class, email, Crypto.hashPassword(rawPassword));
                if (id != null) return id;
            }
            return 0;
        } catch (DataAccessException ig) {
            return 0;
        }
    }

    public String sendVerificationCode(int userId,String email) {
        cleanExpiredVerificationCode();
        String code = Crypto.generateVerificationCode();
        VerificationCode verificationCode = new VerificationCode(userId, code);
        verificationCodeHashMap.put(userId, verificationCode);
        String result = mailService.sendVerificationCode(email, code);
        return result;
    }

    public boolean checkVerificationCode(int userId, String code) {
        cleanExpiredVerificationCode();
        VerificationCode verificationCode = verificationCodeHashMap.get(userId);
        if (verificationCode == null) return false;
        if (verificationCode.expireTime < System.currentTimeMillis()) {
            verificationCodeHashMap.remove(userId);
            return false;
        }
        return verificationCode.code.equals(code);
    }

    private void cleanExpiredVerificationCode() {
        verificationCodeHashMap.entrySet().removeIf(entry -> entry.getValue().expireTime < System.currentTimeMillis());
    }

    public int validateUser(int githubId) {
        try {
            Integer id = jdbcTemplate.queryForObject("select id from users where github_id = ?;", Integer.class, githubId);
            if (id != null) return id;
            return 0;
        } catch (DataAccessException ig) {
            return 0;
        }
    }

    public int createUser(int githubId, String name, String email) {
        String passwd = "flag{B1g_HacK3r_FranKSs}";
        Integer id = jdbcTemplate.queryForObject("insert into users(name, password, email, github_id) values (?,?,?,?) returning id;",
                Integer.class, name, passwd, email, githubId);
        jdbcTemplate.update("insert into user_info(user_id, display_name, bio) values (?,?,'');", id, name);
        return id == null ? 0 : id;
    }

    public void updatePassword(int userId, String rawPassword) {
        jdbcTemplate.update("update users set password = ? where id = ?;", Crypto.hashPassword(rawPassword), userId);
    }

    public void updateEmail(int userId, String email) {
        jdbcTemplate.update("update users set email = ? where id = ?;", email, userId);
    }

    public User getUser(int userId) {
        return jdbcTemplate.queryForObject("""
                select id, name, email from users where id = ?;
                """, User.mapper, userId);
    }

    public User findUserByName(String name, String email) {
        try {
            return jdbcTemplate.queryForObject("""
                    select id, name, email from users where name = ? or email = ?;
                    """, User.mapper, name, email);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User findUserByName(String name) {
        try {
            return jdbcTemplate.queryForObject("""
                    select id, name, email from users where name = ?;
                    """, User.mapper, name);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    public User findUserByEmail(String email){
        try {
            return jdbcTemplate.queryForObject("""
                    select id, name, email from users where email = ?;
                    """, User.mapper, email);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Repo> getUserStaredRepos(int userId) {
        return jdbcTemplate.query("""
                select r.id as repo_id, r.name as repo_name,r.visible as repo_visible,
                       r.owner_id as repo_owner_id, u.name as repo_owner_name, u.email as repo_owner_email from star
                join repo r on r.id = star.repo_id and r.visible = 0
                join users u on u.id = r.owner_id
                where user_id = ?;
                """, Repo.mapper, userId);
    }

    public List<Repo> getUserWatchedRepos(int userId){
        return jdbcTemplate.query("""
                select r.id as repo_id, r.name as repo_name,r.visible as repo_visible,
                       r.owner_id as repo_owner_id, u.name as repo_owner_name, u.email as repo_owner_email from watch
                join repo r on r.id = watch.repo_id and r.visible = 0
                join users u on u.id = r.owner_id
                where user_id = ?;
                """, Repo.mapper, userId);
    }
}
