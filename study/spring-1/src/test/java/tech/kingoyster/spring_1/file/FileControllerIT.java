package tech.kingoyster.spring_1.file;

import java.nio.file.Path;
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

    @Test
    void whenUpload_thenNoError(@TempDir Path tempDir) throws Exception {
        Mockito.when(fileProperties.getDirectory()).thenReturn(tempDir);
        var mockFile =
                new MockMultipartFile("file", "test.txt", "text/plain", "Test file".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/file").file(mockFile))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void whenDownload_thenReturnFile(@TempDir Path tempDir) throws Exception {
        Mockito.when(fileProperties.getDirectory()).thenReturn(tempDir);
        var filename = "file.txt";

        var mockResult =
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/file/" + filename))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(
                                MockMvcResultMatchers.header()
                                        .string(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"file.txt\""))
                        .andExpect(
                                MockMvcResultMatchers.content()
                                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                        .andReturn();

        byte[] contentAsByteArray = mockResult.getResponse().getContentAsByteArray();
        Assertions.assertNotNull(contentAsByteArray);
        Assertions.assertTrue(contentAsByteArray.length > 0);
    }
}
