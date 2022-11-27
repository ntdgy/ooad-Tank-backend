package tank.ooad.fitgub.entity.ci;

import org.springframework.jdbc.core.RowMapper;


public class CiWork {
    public int id;
    public String ciName;
    public String hash;

    public CiWork(int id, String ciName, String hash) {
        this.id = id;
        this.ciName = ciName;
        this.hash = hash;
    }

    public static final RowMapper<CiWork> mapper = (rs, rowNum) ->{
        return new CiWork(rs.getInt("id"), rs.getString("ci_name"), rs.getString("output_hash"));
    };
}
