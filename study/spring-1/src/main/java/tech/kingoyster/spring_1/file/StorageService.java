package tech.kingoyster.spring_1.file;

import java.util.List;

public interface StorageService {
    byte[] getFile(String filename);

    List<String> getAll();

    void saveFile(String filename, byte[] content);
}
