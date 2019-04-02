package file;

import java.nio.file.Path;

public class RFC {
    public static final String CONTENT_TYPE = "text/text";
    public Path path;
    public int id;
    public String title;

    public RFC(Path path) {
        this.path = path.toAbsolutePath();
        String fileName = path.getFileName().toString();
        id = Integer.parseInt(fileName.substring(0, 4));
        title = fileName.substring(4, fileName.lastIndexOf('.')).trim();
    }

    public RFC(int id, String title) {
        this.id = id;
        this.title = title;
    }
}
