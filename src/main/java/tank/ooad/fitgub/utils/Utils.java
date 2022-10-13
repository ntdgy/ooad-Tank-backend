package tank.ooad.fitgub.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
    public static boolean isBinaryFile(String path) throws IOException {
        String type = Files.probeContentType(Path.of(path));
        //type isn't text
        if (type == null) {
            //type couldn't be determined, assume text
            return false;
        } else return !type.startsWith("text");
    }
}
