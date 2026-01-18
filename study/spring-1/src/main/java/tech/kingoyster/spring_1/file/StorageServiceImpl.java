package tech.kingoyster.spring_1.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {
    private final FileProperties fileProperties;

    @Override
    public Resource getFile(String filename) {
        return null;
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
    public void saveFile(String filename, byte[] content) {
        //
    }
}
