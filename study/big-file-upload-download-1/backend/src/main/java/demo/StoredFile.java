package demo;

import java.time.Instant;
import java.util.UUID;

public record StoredFile(
    UUID id,
    String originalName,
    long sizeBytes,
    String status,
    Integer chunkSize,
    Integer totalChunks,
    Instant createdAt
) {}
