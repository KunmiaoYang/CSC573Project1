package file;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;

public class LocalStorage {
    Path root;
    HashMap<Integer, RFC> rfcMap = new HashMap<>();

    public LocalStorage(Path root) {
        this.root = root.toAbsolutePath();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root, "*.txt")) {
            for (Path file: ds) {
                try {
                    RFC rfc = new RFC(file);
                    rfcMap.put(rfc.id, rfc);
                    System.out.println(file.toAbsolutePath().toString());
                } catch (Exception e) {
                    System.err.println("Invalid file: " + file.getFileName());
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public RFC getRFC(int number) {
        return rfcMap.get(number);
    }

    public Collection<RFC> getAllRFC() {
        return rfcMap.values();
    }

    public Path createFile(String filename) throws IOException {
        Path file = Paths.get(root.toString(), filename);
        if (!Files.exists(file)) Files.createFile(file);
        return file;
    }
}
