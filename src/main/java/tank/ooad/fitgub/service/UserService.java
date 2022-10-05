package tank.ooad.fitgub.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.user.User;
import tank.ooad.fitgub.utils.Crypto;

@Component
public class UserService {
    private final JdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    public boolean checkExist(String username, String email) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from users where name = ? or email=?", Integer.class, username, email);
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

    public User getUser(int userId) {
        return jdbcTemplate.queryForObject("""
                select id, name, email from users where id = ?;
                """, User.mapper, userId);
    }
    public User findUser(String name, String email) {
        return jdbcTemplate.queryForObject("""
                select id, name, email from users where name = ? or email = ?;
                """, User.mapper, name, email);
    }
}
