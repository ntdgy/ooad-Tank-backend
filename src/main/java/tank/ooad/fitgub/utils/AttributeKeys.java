package tank.ooad.fitgub.utils;


import org.springframework.lang.NonNull;

import javax.servlet.http.HttpSession;

public enum AttributeKeys {
    USER_ID("user_id", Integer.class, 0);

    private final String key;
    private final Class<?> type;
    private final Object defaultValue;

    AttributeKeys(String key, Class<?> type, Object defaultValue) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public Class<?> getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Object getValue(HttpSession session) {
        Object obj = session.getAttribute(key);
        if (obj == null) {
            if (defaultValue != null) {
                session.setAttribute(key, defaultValue);
                obj = defaultValue;
            }
        }
        return obj;
    }

    public void setValue(HttpSession session, Object value) {
        assert value.getClass().equals(type);
        session.setAttribute(this.key, value);
    }

}
