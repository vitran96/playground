package tech.kingoyster.spring_1.file;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    @GetMapping
    public List<String> listFile() {
        return null;
    }

    @GetMapping(value = "/{filename:.+}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        return null;
    }

    //    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    //    @ResponseStatus(HttpStatus.NO_CONTENT)
    //    public void uploadFile(@RequestParam("file")MultipartFile file) throws IOException {
    //        var filename = file.getOriginalFilename();
    //        var data = file.getBytes();
    //
    //        // TODO:
    //    }

    // TODO: can file upload be a stream?
    // NOTE: @RequestPart allow use to send both BLOB data & JSON
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") Map<String, String> additionalData)
            throws IOException {
        var filename = file.getOriginalFilename();
        var data = file.getBytes();

        // TODO:
    }
}
