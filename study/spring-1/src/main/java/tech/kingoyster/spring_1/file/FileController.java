package tech.kingoyster.spring_1.file;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
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

    // NOTE: below will not reflect the dynamic content type
//    @GetMapping(value = "/{filename:.+}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
//    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
//        var contentAsByteArray = storageService.getFile(filename);
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                .body(new ByteArrayResource(contentAsByteArray));
//    }

    @GetMapping(value = "/{filename:.+}")
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    schema = @Schema(type = "string", format = "binary") // NOTE: force download button
            )
    )
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws IOException {
        var resource = storageService.getFile(filename);

        var type = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
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
    @Operation(
            summary = "Fixed Upload",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            encoding = @Encoding(name = "metadata", contentType = "application/json")
                    )
            )
    )
    public void uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "metadata", required = false) Map<String, String> additionalData)
            throws IOException {
        var filename = file.getOriginalFilename();
        // NOTE: this will load the whole file into memory
        var data = file.getBytes();

        storageService.saveFile(filename, data);
    }
}
