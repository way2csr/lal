package learn.lal.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import learn.lal.model.CurriculumManifestEntry;
import learn.lal.service.CurriculumRegistryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/curriculum")
public class AdminCurriculumController {

    private final CurriculumRegistryService curriculumRegistryService;
    private final ObjectMapper objectMapper;

    public AdminCurriculumController(CurriculumRegistryService curriculumRegistryService, ObjectMapper objectMapper) {
        this.curriculumRegistryService = curriculumRegistryService;
        this.objectMapper = objectMapper;
    }

    /** All manifest rows (including soft-deleted) for admin UI. */
    @GetMapping("/manifest")
    public List<Map<String, Object>> listManifest() throws IOException {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CurriculumManifestEntry e : curriculumRegistryService.listAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("displayName", e.getDisplayName());
            m.put("level", e.getLevel());
            m.put("deleted", e.isDeleted());
            m.put("storageFile", e.getStorageFile());
            if (e.getOriginalStorageFile() != null) m.put("originalStorageFile", e.getOriginalStorageFile());
            if (e.getDeletedBy() != null) m.put("deletedBy", e.getDeletedBy());
            if (e.getDeletedAt() != null) m.put("deletedAt", e.getDeletedAt());
            if (e.getDeletionReason() != null) m.put("deletionReason", e.getDeletionReason());
            out.add(m);
        }
        return out;
    }

    /** Read the raw question map for any curriculum (including inactive/deleted). */
    @GetMapping("/{id}/data")
    public ResponseEntity<Map<String, Object>> getCurriculumData(@PathVariable String id) {
        try {
            Map<String, Object> data = curriculumRegistryService.readCurriculumDataAdmin(id);
            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (IOException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Replace JSON content for one curriculum (same shape as GET /api/questions/curriculum for that id).
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> putCurriculum(@PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            CurriculumRegistryService.validateCurriculumShape(body);
            curriculumRegistryService.writeCurriculumData(id, body);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("status", "OK");
            ok.put("id", id);
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (IOException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Add a new curriculum from JSON file (does not replace existing).
     */
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addCurriculum(
            @RequestParam("file") MultipartFile file,
            @RequestParam("displayName") String displayName,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "id", required = false) String id) {
        if (file == null || file.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "No file");
            return ResponseEntity.badRequest().body(err);
        }
        try {
            Map<String, Object> data = objectMapper.readValue(file.getInputStream(), new TypeReference<Map<String, Object>>() {});
            CurriculumManifestEntry added = curriculumRegistryService.addCurriculum(displayName, level, id, data);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("status", "OK");
            ok.put("id", added.getId());
            ok.put("displayName", added.getDisplayName());
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (IOException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /** Deactivate: hides curriculum from learners without touching the data file. */
    @PostMapping("/{id}/soft-delete")
    public ResponseEntity<Map<String, Object>> softDelete(@PathVariable String id) {
        try {
            curriculumRegistryService.softDelete(id);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("status", "OK");
            ok.put("id", id);
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (IOException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Delete with reason: renames the data file on disk to include timestamp + username,
     * stores the deletion reason in the manifest, and marks the entry as deleted.
     */
    @PostMapping("/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteWithReason(
            @PathVariable String id,
            @RequestParam("reason") String reason) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = (auth != null && auth.isAuthenticated()) ? auth.getName() : "unknown";
        try {
            if (reason == null || reason.isBlank()) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "A deletion reason is required.");
                return ResponseEntity.badRequest().body(err);
            }
            curriculumRegistryService.deleteWithReason(id, reason, deletedBy);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("status", "OK");
            ok.put("id", id);
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (IOException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Permanent delete: physically erases the data file and removes the manifest entry.
     * This action is irreversible. A non-empty reason is required.
     */
    @PostMapping("/{id}/permanent-delete")
    public ResponseEntity<Map<String, Object>> permanentDelete(
            @PathVariable String id,
            @RequestParam("reason") String reason) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = (auth != null && auth.isAuthenticated()) ? auth.getName() : "unknown";
        try {
            if (reason == null || reason.isBlank()) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "A deletion reason is required.");
                return ResponseEntity.badRequest().body(err);
            }
            curriculumRegistryService.permanentDelete(id, reason, deletedBy);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("status", "OK");
            ok.put("id", id);
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (IOException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Map<String, Object>> restore(@PathVariable String id) {
        try {
            curriculumRegistryService.restore(id);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("status", "OK");
            ok.put("id", id);
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (IOException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }
}
