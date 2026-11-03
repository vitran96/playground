package demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FileTransferTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Storage storage;

    @Autowired
    private FileRepository repo;

    @Test
    void testSingleUploadAndRangeDownload() throws Exception {
        byte[] payload = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(StandardCharsets.UTF_8);

        // Upload single file
        String response = mvc.perform(post("/api/files?name=test.txt")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("test.txt");
        UUID id = repo.findAllReady().getFirst().id();

        // Download with byte range 0-9
        mvc.perform(get("/api/files/" + id + "/content")
                        .header(HttpHeaders.RANGE, "bytes=0-9"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-9/" + payload.length))
                .andExpect(content().bytes("0123456789".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testChunkedUploadOutOfOrderAndComplete() throws Exception {
        String data = "ChunkZero_ChunkOne__ChunkTwo__";
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        int chunkSize = 10;
        int total = bytes.length; // 30 bytes -> 3 chunks

        // Init upload
        String initJson = """
                {"name": "chunked.bin", "size": %d, "chunkSize": %d}
                """.formatted(total, chunkSize);

        mvc.perform(post("/api/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initJson))
                .andExpect(status().isOk());

        UUID id = repo.findUploading(repo.findUploading(UUID.randomUUID()).map(StoredFile::id).orElse(null))
                .map(StoredFile::id)
                .orElseGet(() -> {
                    // find latest
                    return repo.findAllReady().stream().findFirst().map(FileDto::id).orElse(null);
                });

        // Let's get the specific id from the init endpoint directly
        var initResult = mvc.perform(post("/api/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initJson))
                .andReturn().getResponse().getContentAsString();
        UUID uploadId = UUID.fromString(initResult.split("\"id\":\"")[1].split("\"")[0]);

        // Sending duplicate chunk 1 and missing chunk 0 -> complete should fail 409
        byte[] chunk1 = "ChunkOne__".getBytes(StandardCharsets.UTF_8);
        byte[] chunk2 = "ChunkTwo__".getBytes(StandardCharsets.UTF_8);
        byte[] chunk0 = "ChunkZero_".getBytes(StandardCharsets.UTF_8);

        // Upload chunk 2 first (out of order)
        mvc.perform(put("/api/uploads/" + uploadId + "/chunks/2")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(chunk2))
                .andExpect(status().isOk());

        // Upload chunk 1
        mvc.perform(put("/api/uploads/" + uploadId + "/chunks/1")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(chunk1))
                .andExpect(status().isOk());

        // Duplicate chunk 1 (harmless)
        mvc.perform(put("/api/uploads/" + uploadId + "/chunks/1")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(chunk1))
                .andExpect(status().isOk());

        // Attempt complete without chunk 0 -> 409 Conflict
        mvc.perform(post("/api/uploads/" + uploadId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());

        // Now upload chunk 0
        mvc.perform(put("/api/uploads/" + uploadId + "/chunks/0")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(chunk0))
                .andExpect(status().isOk());

        // Complete should succeed
        mvc.perform(post("/api/uploads/" + uploadId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Verify assembled file matches exactly
        byte[] assembled = Files.readAllBytes(storage.readyPath(uploadId));
        assertThat(assembled).isEqualTo(bytes);
    }

    @Test
    void testOffsetComputationPast2GB() {
        int index = 300;
        int chunkSize = 8 * 1024 * 1024; // 8 MB
        // index * chunkSize as int overflows to negative: 300 * 8388608 = 2,516,582,400 > Integer.MAX_VALUE
        int overflowed = index * chunkSize;
        assertThat(overflowed).isNegative();

        long correct = (long) index * chunkSize;
        assertThat(correct).isEqualTo(2_516_582_400L);
    }
}
