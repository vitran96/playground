package tech.kingoyster.spring_1.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

@ExtendWith(MockitoExtension.class)
public class StorageServiceTest {
    @InjectMocks private StorageService storageService;

    @Mock private FileProperties fileProperties;

    @Test
    void whenCreateFile_thenFileCreatedAndReturnNothing(@TempDir Path tempDir) {
        Assertions.fail("not implemented");
    }

    @Test
    void whenGetFile_thenReturnResource(@TempDir Path tempDir) {
        Assertions.fail("not implemented");
    }

    @Test
    void whenGetNonExistsFile_thenThrowError(@TempDir Path tempDir) {
        Assertions.fail("not implemented");
    }

    @Test
    void whenGetListOfFileOnNewDir_thenReturnEmptyList(@TempDir Path tempDir) {
        Assertions.fail("not implemented");
    }

    @Test
    void whenGetListOfFile_thenReturnListWithSomeItems(@TempDir Path tempDir) {
        Assertions.fail("not implemented");
    }
}
