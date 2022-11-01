package tank.ooad.fitgub.entity.ci;

import java.util.ArrayList;
import java.util.List;

public class Job {
    public String name;
    public String runs_on;
    public List<Step> steps;

    public Job() {
        this.steps = new ArrayList<>();
    }

}

