package tech.kingoyster.spring_1.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tech.kingoyster.spring_1.exception.FileAlreadyExistsException;
import tech.kingoyster.spring_1.exception.FileNotFoundException;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {
    private final FileProperties fileProperties;

    @Override
    public Resource getFile(String filename) throws IOException {
        var path = fileProperties.getDirectory().resolve(filename);
        if (Files.notExists(path)) {
            throw new FileNotFoundException(filename);
        } else {
            if (Files.notExists(fileProperties.getDirectory())) {
                Files.createDirectory(fileProperties.getDirectory().toAbsolutePath());
            }
        }

        return new FileSystemResource(path);
    }

    @Override
    public List<String> getAll() throws IOException {
        if (Files.notExists(fileProperties.getDirectory())) {
            return List.of();
        }

        try (var fileStream = Files.list(fileProperties.getDirectory())) {
            return fileStream.map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
        }
    }

    @Override
    public void saveFile(String filename, byte[] content) throws IOException {
        if (Files.notExists(fileProperties.getDirectory())) {
            Files.createDirectory(fileProperties.getDirectory());
        }

        var path = fileProperties.getDirectory().resolve(filename);
        if (Files.exists(path)) {
            throw new FileAlreadyExistsException(filename);
        }

        Files.write(path, content);
    }
}
