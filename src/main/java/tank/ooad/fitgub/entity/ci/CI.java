package tank.ooad.fitgub.entity.ci;

import java.util.ArrayList;
import java.util.List;

public class CI {
    public String name;
    public List<Job> jobs;

    public CI() {
        this.jobs = new ArrayList<>();
    }
}
