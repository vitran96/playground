package com.example.demo;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileService {

    private final StoredFileRepository repo;
    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProps props;

    public FileService(StoredFileRepository repo, S3Client s3, S3Presigner presigner, StorageProps props) {
        this.repo = repo;
        this.s3 = s3;
        this.presigner = presigner;
        this.props = props;
    }

    public StoredFile uploadProxy(MultipartFile file) throws IOException {
        UUID id = UUID.randomUUID();
        String key = "uploads/" + id;
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        StoredFile stored = new StoredFile(
                id,
                key,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                "READY",
                Instant.now()
        );
        return repo.save(stored);
    }

    public List<StoredFile> listFiles() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public StoredFile getFile(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    public ResponseInputStream<GetObjectResponse> getStream(StoredFile file) {
        return s3.getObject(GetObjectRequest.builder()
                .bucket(props.bucket())
                .key(file.getObjectKey())
                .build());
    }

    public String getPresignedDownloadUrl(UUID id) {
        StoredFile f = getFile(id);
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(props.bucket())
                .key(f.getObjectKey())
                .responseContentDisposition("attachment; filename=\"" + f.getOriginalName() + "\"")
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(get));
        return presigned.url().toString();
    }

    public Map<String, Object> createPresignedUpload(String name, String contentType, Long size) {
        UUID id = UUID.randomUUID();
        String key = "uploads/" + id;
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.bucket())
                .key(key)
                .contentType(contentType)
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(put));

        StoredFile stored = new StoredFile(
                id,
                key,
                name,
                contentType,
                size,
                "PENDING",
                Instant.now()
        );
        repo.save(stored);
        return Map.of("id", id, "url", presigned.url().toString());
    }

    public StoredFile confirmUpload(UUID id) {
        StoredFile f = getFile(id);
        HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder()
                .bucket(props.bucket())
                .key(f.getObjectKey())
                .build());
        f.setSizeBytes(head.contentLength());
        f.setStatus("READY");
        return repo.save(f);
    }

    public void deleteFile(UUID id) {
        StoredFile f = getFile(id);
        // ponytail: two stores with no 2PC; S3 delete first to prevent orphan DB pointer
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.bucket())
                .key(f.getObjectKey())
                .build());
        repo.delete(f);
    }
}
