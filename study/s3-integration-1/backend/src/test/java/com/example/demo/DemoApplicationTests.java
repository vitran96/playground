package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private FileService fileService;

    @Autowired
    private StoredFileRepository repository;

    @Test
    void testAllStoragePatterns() throws Exception {
        byte[] payload = "Hello SeaweedFS!".getBytes(StandardCharsets.UTF_8);

        // 1. Proxy upload
        MockMultipartFile multipart = new MockMultipartFile("file", "test.txt", "text/plain", payload);
        StoredFile uploaded = fileService.uploadProxy(multipart);
        assertNotNull(uploaded.getId());
        assertEquals("READY", uploaded.getStatus());

        // 2. List & get stream
        try (var stream = fileService.getStream(uploaded)) {
            assertArrayEquals(payload, stream.readAllBytes());
        }

        // 3. Presigned download
        String downloadUrl = fileService.getPresignedDownloadUrl(uploaded.getId());
        assertNotNull(downloadUrl);
        HttpClient http = HttpClient.newHttpClient();
        var dlResponse = http.send(HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, dlResponse.statusCode());
        assertArrayEquals(payload, dlResponse.body());

        // 4. Presigned upload + direct PUT to SeaweedFS + confirm
        Map<String, Object> presignedUpload = fileService.createPresignedUpload("direct.txt", "text/plain", (long) payload.length);
        String putUrl = (String) presignedUpload.get("url");
        var uploadId = (java.util.UUID) presignedUpload.get("id");

        var putResponse = http.send(HttpRequest.newBuilder(URI.create(putUrl))
                .header("Content-Type", "text/plain")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(200, putResponse.statusCode());

        StoredFile confirmed = fileService.confirmUpload(uploadId);
        assertEquals("READY", confirmed.getStatus());
        assertEquals((long) payload.length, confirmed.getSizeBytes());

        // 5. Delete both
        fileService.deleteFile(uploaded.getId());
        fileService.deleteFile(confirmed.getId());
        assertTrue(repository.findById(uploaded.getId()).isEmpty());
        assertTrue(repository.findById(confirmed.getId()).isEmpty());
    }
}
