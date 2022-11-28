package tank.ooad.fitgub.entity.ci;

import org.springframework.jdbc.core.RowMapper;


public class CiWork {
    public int id;
    public String ciName;
    public String hash;
    public long createTime;

    public CiWork(int id, String ciName, String hash, long createTime) {
        this.id = id;
        this.ciName = ciName;
        this.hash = hash;
        this.createTime = createTime;
    }

    public static final RowMapper<CiWork> mapper = (rs, rowNum) ->{
        return new CiWork(rs.getInt("id"), rs.getString("ci_name"), rs.getString("output_hash")
                , rs.getLong("created_at"));
    };
}
