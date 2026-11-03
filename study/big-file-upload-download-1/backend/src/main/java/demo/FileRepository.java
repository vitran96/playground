package demo;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FileRepository {
    private final JdbcClient jdbc;

    private static final RowMapper<StoredFile> FILE_MAPPER = (rs, i) -> new StoredFile(
            rs.getObject("id", UUID.class),
            rs.getString("original_name"),
            rs.getLong("size_bytes"),
            rs.getString("status"),
            rs.getObject("chunk_size", Integer.class),
            rs.getObject("total_chunks", Integer.class),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
    );

    private static final RowMapper<FileDto> DTO_MAPPER = (rs, i) -> new FileDto(
            rs.getObject("id", UUID.class),
            rs.getString("original_name"),
            rs.getLong("size_bytes")
    );

    public FileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<FileDto> findAllReady() {
        return jdbc.sql("select id, original_name, size_bytes from stored_file where status = 'READY' order by created_at desc")
                .query(DTO_MAPPER)
                .list();
    }

    public FileDto insertReady(UUID id, String name, long size) {
        jdbc.sql("insert into stored_file (id, original_name, size_bytes, status) values (?, ?, ?, 'READY')")
                .params(id, name, size)
                .update();
        return new FileDto(id, name, size);
    }

    public void insertUploading(UUID id, String name, long size, int chunkSize, int totalChunks) {
        jdbc.sql("insert into stored_file (id, original_name, size_bytes, status, chunk_size, total_chunks) values (?, ?, ?, 'UPLOADING', ?, ?)")
                .params(id, name, size, chunkSize, totalChunks)
                .update();
    }

    public Optional<StoredFile> findUploading(UUID id) {
        return jdbc.sql("select id, original_name, size_bytes, status, chunk_size, total_chunks, created_at from stored_file where id = ? and status = 'UPLOADING'")
                .param(id)
                .query(FILE_MAPPER)
                .optional();
    }

    public Optional<StoredFile> findReady(UUID id) {
        return jdbc.sql("select id, original_name, size_bytes, status, chunk_size, total_chunks, created_at from stored_file where id = ? and status = 'READY'")
                .param(id)
                .query(FILE_MAPPER)
                .optional();
    }

    public void markChunk(UUID id, int chunkIndex) {
        jdbc.sql("insert into upload_chunk (file_id, chunk_index) values (?, ?) on conflict do nothing")
                .params(id, chunkIndex)
                .update();
    }

    public int countChunks(UUID id) {
        return jdbc.sql("select count(*) from upload_chunk where file_id = ?")
                .param(id)
                .query(Integer.class)
                .single();
    }

    public void markReady(UUID id) {
        jdbc.sql("update stored_file set status = 'READY' where id = ?")
                .param(id)
                .update();
    }

    public void delete(UUID id) {
        jdbc.sql("delete from stored_file where id = ?")
                .param(id)
                .update();
    }
}
