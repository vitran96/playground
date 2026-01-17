package tech.kingoyster.spring_1.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

@ExtendWith(MockitoExtension.class)
public class StorageServiceTest {
    @InjectMocks private StorageService storageService;

    @Test
    void whenCreateFile_thenFileCreated(@TempDir Path tempDir) {
        Assertions.fail("not implemented");
    }
}
