package hr.fer.artconnect.file.web;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ApiPaths;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.file.service.FileAccessService;
import hr.fer.artconnect.file.service.FileDownload;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(FileController.PATH)
public class FileController {
    static final String PATH = ApiPaths.V1 + "/files";

    private static final String NOSNIFF_HEADER = "X-Content-Type-Options";
    private static final String PUBLIC_CACHE = "public, max-age=3600";
    private static final String PRIVATE_CACHE = "private, no-store";

    private final FileAccessService files;

    public FileController(FileAccessService files) {
        this.files = files;
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        FileDownload download = files.resolve(parseFileId(fileId));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(download.originalName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.CACHE_CONTROL,
                        download.publiclyCacheable() ? PUBLIC_CACHE : PRIVATE_CACHE)
                .header(NOSNIFF_HEADER, "nosniff")
                .body(download.resource());
    }

    private static UUID parseFileId(String fileId) {
        try {
            return UUID.fromString(fileId);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Datoteka nije pronađena.");
        }
    }
}
