package tech.kingoyster.spring_1.file;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

public interface StorageService {
    /**
     *
     * @param filename existing filename
     * @return {@link org.springframework.core.io.FileSystemResource}
     */
    Resource getFile(String filename) throws IOException;

    List<String> getAll() throws IOException;

    void saveFile(String filename, byte[] content) throws IOException;
}
