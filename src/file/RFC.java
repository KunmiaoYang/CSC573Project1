package file;

import java.nio.file.Path;

public class RFC {
    Path path;
    int id;
    String title;

    public RFC(Path path) {
        this.path = path.toAbsolutePath();
        String fileName = path.getFileName().toString();
        id = Integer.parseInt(fileName.substring(0, 4));
        title = fileName.substring(4, fileName.lastIndexOf('.')).trim();
    }
}
