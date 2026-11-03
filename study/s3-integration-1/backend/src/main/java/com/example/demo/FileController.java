package com.example.demo;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoredFile> uploadProxy(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.uploadProxy(file));
    }

    @GetMapping
    public List<StoredFile> listFiles() {
        return service.listFiles();
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> downloadContent(@PathVariable UUID id) {
        StoredFile f = service.getFile(id);
        ResponseInputStream<GetObjectResponse> stream = service.getStream(f);
        MediaType mediaType = f.getContentType() != null ? MediaType.parseMediaType(f.getContentType()) : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(f.getSizeBytes() != null ? f.getSizeBytes() : stream.response().contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + f.getOriginalName() + "\"")
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/{id}/link")
    public Map<String, String> getDownloadLink(@PathVariable UUID id) {
        return Map.of("url", service.getPresignedDownloadUrl(id));
    }

    public record UploadUrlRequest(String name, String contentType, Long size) {}

    @PostMapping("/upload-url")
    public Map<String, Object> createUploadUrl(@RequestBody UploadUrlRequest req) {
        return service.createPresignedUpload(req.name(), req.contentType(), req.size());
    }

    @PostMapping("/{id}/confirm")
    public StoredFile confirmUpload(@PathVariable UUID id) {
        return service.confirmUpload(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable UUID id) {
        service.deleteFile(id);
    }
}
