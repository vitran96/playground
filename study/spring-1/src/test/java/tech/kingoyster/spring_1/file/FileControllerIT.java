package tech.kingoyster.spring_1.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(
        properties = {
            "spring.liquibase.enabled=false", // Don't run migration
            "spring.jpa.hibernate.ddl-auto=none", // Don't validate JPA
            "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        })
@AutoConfigureMockMvc(addFilters = false)
public class FileControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private FileProperties fileProperties;

//    NOTE: in spring-boot test, we can get objectMapper automatically
//    @Autowired
//    private ObjectMapper objectMapper;

    @Test
    void whenGetListOfFileOnEmptyDir_thenReturnEmptyList(@TempDir Path tempDir) throws Exception {
        Mockito.when(fileProperties.getDirectory()).thenReturn(tempDir);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files"))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
    }

    @Test
    void whenGetListOfFileOnNonExistDir_thenReturnEmptyList(@TempDir Path tempDir) throws Exception {
        Mockito.when(fileProperties.getDirectory()).thenReturn(Path.of("non-exists").toAbsolutePath());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
    }

    @Test
    void whenGetListOfNullPath_thenThrowError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError());
    }

    @Test
    void whenGetListOfFileOf2Files_thenReturnListOf2Files(@TempDir Path tempDir) throws Exception {
        Mockito.when(fileProperties.getDirectory()).thenReturn(tempDir);

        Files.write(fileProperties.getDirectory().resolve("file1.txt"), "content 1".getBytes());
        Files.write(fileProperties.getDirectory().resolve("file2.txt"), "content 2".getBytes());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.containsInAnyOrder("file1.txt", "file2.txt")));
    }

    @Test
    void whenUpload_thenNoError(@TempDir Path tempDir) throws Exception {
        Mockito.when(fileProperties.getDirectory()).thenReturn(tempDir);
        var mockFile =
                new MockMultipartFile("file", "test.txt", "text/plain", "Test file".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/files").file(mockFile))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void whenUploadOversizeFile_thenThrowError(@TempDir Path tempDir) {
        Assertions.fail("not implement");
    }

    @Test
    void whenDownload_thenReturnFile(@TempDir Path tempDir) throws Exception {
        Mockito.when(fileProperties.getDirectory()).thenReturn(tempDir);
        var filename = "file.txt";

        Files.write(fileProperties.getDirectory().resolve(filename), "content".getBytes());

        var mockResult =
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files/" + filename))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(
                                MockMvcResultMatchers.header()
                                        .string(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"file.txt\""))
                        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_PLAIN_VALUE))
                        .andReturn();

        byte[] contentAsByteArray = mockResult.getResponse().getContentAsByteArray();
        Assertions.assertNotNull(contentAsByteArray);
        Assertions.assertTrue(contentAsByteArray.length > 0);
    }

    @Test
    void whenDownloadNonExistsFile_thenThrowNotFound(@TempDir Path tempDir) throws Exception {
        Assertions.fail("not implemented");
    }

    @Test
    void whenDownloadOversizeFile_thenThrowError(@TempDir Path tempDir) {
        Assertions.fail("not implement");
    }
}
