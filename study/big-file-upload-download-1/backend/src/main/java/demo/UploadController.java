package demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@RestController
public class UploadController {
    private final FileRepository repo;
    private final Storage storage;

    public record InitUploadRequest(String name, long size, int chunkSize) {}
    public record InitUploadResponse(UUID id, int totalChunks) {}

    public UploadController(FileRepository repo, Storage storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @PostMapping("/api/uploads")
    public InitUploadResponse init(@RequestBody InitUploadRequest req) {
        if (req.chunkSize() <= 0 || req.size() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid size or chunk size");
        }
        int totalChunks = req.size() == 0 ? 1 : (int) Math.ceil((double) req.size() / req.chunkSize());
        UUID id = UUID.randomUUID();
        repo.insertUploading(id, req.name(), req.size(), req.chunkSize(), totalChunks);
        return new InitUploadResponse(id, totalChunks);
    }

    @PutMapping(value = "/api/uploads/{id}/chunks/{index}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void chunk(@PathVariable UUID id, @PathVariable int index, HttpServletRequest request) throws IOException {
        StoredFile f = repo.findUploading(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "upload not found"));

        // ponytail: cast to long first to avoid integer overflow past 2 GB
        long offset = (long) index * f.chunkSize();
        long expected = Math.min((long) f.chunkSize(), f.sizeBytes() - offset);

        if (index < 0 || index >= f.totalChunks() || offset >= f.sizeBytes() || expected <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chunk index out of bounds");
        }

        try (FileChannel out = FileChannel.open(storage.partPath(id), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             ReadableByteChannel in = Channels.newChannel(request.getInputStream())) {
            long written = 0;
            while (written < expected) {
                long w = out.transferFrom(in, offset + written, expected - written);
                if (w <= 0) break;
                written += w;
            }
            if (written != expected) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "short chunk: expected " + expected + " but got " + written);
            }
        }
        repo.markChunk(id, index);
    }

    @PostMapping("/api/uploads/{id}/complete")
    public FileDto complete(@PathVariable UUID id) throws IOException {
        StoredFile f = repo.findUploading(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "upload not found"));

        if (repo.countChunks(id) != f.totalChunks()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "missing chunks");
        }

        Path part = storage.partPath(id);
        Path ready = storage.readyPath(id);
        try {
            Files.move(part, ready, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(part, ready, StandardCopyOption.REPLACE_EXISTING);
        }
        repo.markReady(id);
        return new FileDto(id, f.originalName(), f.sizeBytes());
    }
}
