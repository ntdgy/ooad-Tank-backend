package tank.ooad.fitgub.entity.git;

public class GitTreeEntry {
    public String name;
    public String hash;
    public boolean isFolder;

    public GitTreeEntry() {

    }

    public GitTreeEntry(String name, String hash, boolean isFolder) {
        this.name = name;
        this.hash = hash;
        this.isFolder = isFolder;
    }
}
