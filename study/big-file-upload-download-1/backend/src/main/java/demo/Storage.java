package demo;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class Storage {
    private final Path base;

    public Storage(@Value("${app.storage-dir:./data/files}") String dir) {
        this.base = Path.of(dir);
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(base);
    }

    public Path readyPath(UUID id) {
        return base.resolve(id.toString());
    }

    public Path partPath(UUID id) {
        return base.resolve(id + ".part");
    }

    public void delete(UUID id) throws IOException {
        Files.deleteIfExists(readyPath(id));
        Files.deleteIfExists(partPath(id));
    }
}
