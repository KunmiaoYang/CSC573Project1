package file;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;

public class LocalStorage {
    Path root;
    HashMap<Integer, RFC> rfcMap = new HashMap<>();

    public LocalStorage(Path root) {
        this.root = root;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root, "*.txt")) {
            for (Path file: ds) {
                RFC rfc = new RFC(file);
                rfcMap.put(rfc.id, rfc);
                System.out.println(file.toAbsolutePath().toString());
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public Collection<RFC> getAllRFC() {
        return rfcMap.values();
    }
}
