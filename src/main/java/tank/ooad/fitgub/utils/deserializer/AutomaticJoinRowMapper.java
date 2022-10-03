package tank.ooad.fitgub.utils.deserializer;

import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class AutomaticJoinRowMapper<T> implements RowMapper<T> {

    private static final ConcurrentHashMap<Class<?>, SynthesisRowMapper> cacheMapper = new ConcurrentHashMap<Class<?>, SynthesisRowMapper>();

    public static class SynthesisRowMapper {
        private final HashMap<String, Field> mapper = new HashMap<>();
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        return null;
    }
}
