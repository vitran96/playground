package demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
public class FileController {
    private final FileRepository repo;
    private final Storage storage;

    public FileController(FileRepository repo, Storage storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @GetMapping("/api/files")
    public List<FileDto> list() {
        return repo.findAllReady();
    }

    @PostMapping(value = "/api/files", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public FileDto upload(@RequestParam String name, HttpServletRequest request) throws IOException {
        UUID id = UUID.randomUUID();
        Path target = storage.readyPath(id);
        try (InputStream in = request.getInputStream()) {
            long size = Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return repo.insertReady(id, name, size);
        } catch (Exception e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    @GetMapping(value = "/api/files/{id}/content", headers = "Range")
    public ResponseEntity<ResourceRegion> rangeContent(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean attachment,
            @RequestHeader(HttpHeaders.RANGE) String rangeHeader) {
        StoredFile f = repo.findReady(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        Path path = storage.readyPath(id);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File content not found on disk");
        }

        Resource resource = new FileSystemResource(path);
        ContentDisposition cd = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(f.originalName(), StandardCharsets.UTF_8)
                .build();

        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        HttpRange range = ranges.isEmpty() ? HttpRange.createByteRange(0) : ranges.getFirst();
        ResourceRegion region = range.toResourceRegion(resource);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(region);
    }

    @GetMapping("/api/files/{id}/content")
    public ResponseEntity<Resource> fullContent(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean attachment) {
        StoredFile f = repo.findReady(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        Path path = storage.readyPath(id);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File content not found on disk");
        }

        Resource resource = new FileSystemResource(path);
        ContentDisposition cd = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(f.originalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/api/files/{id}")
    public void delete(@PathVariable UUID id) throws IOException {
        repo.delete(id);
        storage.delete(id);
    }
}
