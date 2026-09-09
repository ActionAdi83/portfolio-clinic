package edu.clinic.controllers;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service photo storage — GridFS, the same approach portfolio-shop's
 * ProductImageController uses for product photos.
 *
 * Upload is a POST under /api/admin/**, so SecurityConfig already restricts it
 * to clinic-admin. Serving is under /api/services/images/**, which
 * SecurityConfig permits alongside the rest of the public catalogue.
 */
@RestController
public class ServiceImageController {

    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations gridFsOperations;

    public ServiceImageController(GridFsTemplate gridFsTemplate, GridFsOperations gridFsOperations) {
        this.gridFsTemplate = gridFsTemplate;
        this.gridFsOperations = gridFsOperations;
    }

    @PostMapping("/api/admin/services/images")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (file.isEmpty() || contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are accepted"));
        }

        ObjectId id = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), contentType);
        // Stored as a path relative to the API; the frontend resolves it against
        // its own base URL.
        String url = "services/images/" + id.toHexString();
        return ResponseEntity.ok(Map.of("id", id.toHexString(), "url", url));
    }

    @GetMapping("/api/services/images/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id) throws IOException {
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(id))));
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        GridFsResource resource = gridFsOperations.getResource(file);
        String contentType = resource.getContentType() != null ? resource.getContentType() : MediaType.IMAGE_JPEG_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                // Content-addressed by id, so it can be cached hard.
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(resource.getInputStream().readAllBytes());
    }
}
