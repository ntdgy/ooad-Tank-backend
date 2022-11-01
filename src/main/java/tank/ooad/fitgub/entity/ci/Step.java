package tank.ooad.fitgub.entity.ci;


import java.util.ArrayList;
import java.util.List;

public class Step {
    public String name;
    public List<String> run;
    public Step() {
        this.run = new ArrayList<>();
    }
}

