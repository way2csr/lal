package learn.lal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import learn.lal.model.CurriculumManifestEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Multiple curricula: a manifest lists entries; each curriculum's question map lives in
 * {@code files/{storageFile}}. Soft-delete only flips a flag — data files are never deleted.
 */
@Service
public class CurriculumRegistryService {

    private final ObjectMapper objectMapper;

    @Value("${app.curriculum.dir:data/curricula}")
    private String curriculumDir;

    /** If present and manifest is missing, import this legacy single-file curriculum once. */
    @Value("${app.curriculum.legacy-file:data/abacus-curriculum.json}")
    private String legacyCurriculumFile;

    private volatile boolean bootstrapped;

    public CurriculumRegistryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Path curriculaRoot() {
        return Paths.get(curriculumDir).toAbsolutePath().normalize();
    }

    public Path manifestPath() {
        return curriculaRoot().resolve("manifest.json");
    }

    public Path filesDir() {
        return curriculaRoot().resolve("files");
    }

    public synchronized void ensureBootstrapped() throws IOException {
        if (bootstrapped) {
            return;
        }
        Path root = curriculaRoot();
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        Path files = filesDir();
        if (!Files.exists(files)) {
            Files.createDirectories(files);
        }

        if (Files.exists(manifestPath())) {
            bootstrapped = true;
            return;
        }

        List<CurriculumManifestEntry> manifest = new ArrayList<>();
        Path legacy = Paths.get(legacyCurriculumFile).toAbsolutePath().normalize();
        if (Files.exists(legacy)) {
            Path dest = files.resolve("default.json");
            Files.copy(legacy, dest, StandardCopyOption.REPLACE_EXISTING);
            CurriculumManifestEntry e = new CurriculumManifestEntry();
            e.setId("default");
            e.setDisplayName("Imported curriculum");
            e.setLevel("");
            e.setStorageFile("default.json");
            e.setDeleted(false);
            manifest.add(e);
        } else {
            ClassPathResource resource = new ClassPathResource("questions/level2.json");
            if (!resource.exists()) {
                throw new IOException("Classpath questions/level2.json not found");
            }
            Path dest = files.resolve("level2.json");
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            CurriculumManifestEntry e = new CurriculumManifestEntry();
            e.setId("level2");
            e.setDisplayName("Level 2");
            e.setLevel("Level 2");
            e.setStorageFile("level2.json");
            e.setDeleted(false);
            manifest.add(e);
        }

        writeManifest(manifest);
        bootstrapped = true;
    }

    public List<CurriculumManifestEntry> readManifest() throws IOException {
        ensureBootstrapped();
        if (!Files.exists(manifestPath())) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(Files.newInputStream(manifestPath()),
                new TypeReference<List<CurriculumManifestEntry>>() {});
    }

    public void writeManifest(List<CurriculumManifestEntry> entries) throws IOException {
        Path root = curriculaRoot();
        Files.createDirectories(root);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath().toFile(), entries);
    }

    public List<CurriculumManifestEntry> listActive() throws IOException {
        return readManifest().stream()
                .filter(e -> !e.isDeleted())
                .sorted(Comparator.comparing(CurriculumManifestEntry::getDisplayName,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    public List<CurriculumManifestEntry> listAll() throws IOException {
        return readManifest().stream()
                .sorted(Comparator.comparing(CurriculumManifestEntry::getDisplayName,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    public Optional<CurriculumManifestEntry> findById(String id) throws IOException {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return readManifest().stream().filter(e -> id.equals(e.getId())).findFirst();
    }

    public String resolveDefaultActiveId() throws IOException {
        List<CurriculumManifestEntry> active = listActive();
        if (active.isEmpty()) {
            throw new IOException("No active curriculum in manifest");
        }
        return active.get(0).getId();
    }

    public Map<String, Object> readCurriculumData(String curriculumId) throws IOException {
        ensureBootstrapped();
        Optional<CurriculumManifestEntry> entry = findById(curriculumId);
        if (entry.isEmpty() || entry.get().isDeleted()) {
            throw new IOException("Curriculum not found or not available: " + curriculumId);
        }
        Path dataFile = filesDir().resolve(entry.get().getStorageFile());
        if (!Files.exists(dataFile)) {
            throw new IOException("Curriculum data file missing: " + dataFile);
        }
        return objectMapper.readValue(Files.newInputStream(dataFile), new TypeReference<Map<String, Object>>() {});
    }

    /** Admin-only read — works even for deleted/inactive entries. */
    public Map<String, Object> readCurriculumDataAdmin(String curriculumId) throws IOException {
        ensureBootstrapped();
        Optional<CurriculumManifestEntry> entry = findById(curriculumId);
        if (entry.isEmpty()) {
            throw new IllegalArgumentException("Unknown curriculum id: " + curriculumId);
        }
        Path dataFile = filesDir().resolve(entry.get().getStorageFile());
        if (!Files.exists(dataFile)) {
            throw new IOException("Curriculum data file missing: " + dataFile);
        }
        return objectMapper.readValue(Files.newInputStream(dataFile), new TypeReference<Map<String, Object>>() {});
    }

    public void writeCurriculumData(String curriculumId, Map<String, Object> data) throws IOException {
        ensureBootstrapped();
        Optional<CurriculumManifestEntry> entry = findById(curriculumId);
        if (entry.isEmpty()) {
            throw new IOException("Unknown curriculum id: " + curriculumId);
        }
        Path dataFile = filesDir().resolve(entry.get().getStorageFile());
        Files.createDirectories(dataFile.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile.toFile(), data);
    }

    public CurriculumManifestEntry addCurriculum(String displayName, String level, String requestedId,
            Map<String, Object> data) throws IOException {
        ensureBootstrapped();
        validateCurriculumShape(data);
        String id = normalizeOrGenerateId(requestedId, displayName);
        List<CurriculumManifestEntry> manifest = readManifest();
        if (manifest.stream().anyMatch(e -> id.equals(e.getId()))) {
            throw new IllegalArgumentException("Curriculum id already exists: " + id);
        }
        String storageFile = id + ".json";
        Path dest = filesDir().resolve(storageFile);
        if (Files.exists(dest)) {
            storageFile = id + "-" + UUID.randomUUID().toString().substring(0, 8) + ".json";
            dest = filesDir().resolve(storageFile);
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(dest.toFile(), data);

        CurriculumManifestEntry e = new CurriculumManifestEntry();
        e.setId(id);
        e.setDisplayName(displayName == null || displayName.isBlank() ? id : displayName.trim());
        e.setLevel(level == null ? "" : level.trim());
        e.setStorageFile(storageFile);
        e.setDeleted(false);
        manifest.add(e);
        writeManifest(manifest);
        return e;
    }

    public void softDelete(String id) throws IOException {
        List<CurriculumManifestEntry> manifest = readManifest();
        boolean found = false;
        for (CurriculumManifestEntry e : manifest) {
            if (id.equals(e.getId())) {
                e.setDeleted(true);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown curriculum id: " + id);
        }
        long active = manifest.stream().filter(e -> !e.isDeleted()).count();
        if (active == 0) {
            throw new IllegalArgumentException("Cannot deactivate the last active curriculum");
        }
        writeManifest(manifest);
    }

    /**
     * Soft-deletes a curriculum with a reason: the data file is renamed on disk to include
     * the deletion timestamp and the username. The entry stays in the manifest with full
     * audit info so it can be reviewed later.
     */
    public void deleteWithReason(String id, String reason, String deletedBy) throws IOException {
        ensureBootstrapped();
        List<CurriculumManifestEntry> manifest = readManifest();
        CurriculumManifestEntry target = null;
        for (CurriculumManifestEntry e : manifest) {
            if (id.equals(e.getId())) { target = e; break; }
        }
        if (target == null) {
            throw new IllegalArgumentException("Unknown curriculum id: " + id);
        }
        long active = manifest.stream().filter(e -> !e.isDeleted()).count();
        if (!target.isDeleted() && active <= 1) {
            throw new IllegalArgumentException("Cannot delete the last active curriculum");
        }

        String nowStr = ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeUser = (deletedBy == null || deletedBy.isBlank() ? "unknown"
                : deletedBy.replaceAll("[^a-zA-Z0-9_-]", "_"));

        String originalFile = target.getStorageFile();
        String stem = originalFile.endsWith(".json")
                ? originalFile.substring(0, originalFile.length() - 5)
                : originalFile;
        String renamedFile = stem + "__deleted_" + nowStr + "_" + safeUser + ".json";

        Path srcPath = filesDir().resolve(originalFile);
        Path dstPath = filesDir().resolve(renamedFile);
        if (Files.exists(srcPath)) {
            Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
        }

        target.setDeleted(true);
        target.setOriginalStorageFile(originalFile);
        target.setStorageFile(renamedFile);
        target.setDeletedBy(deletedBy);
        target.setDeletedAt(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        target.setDeletionReason(reason == null ? "" : reason.trim());
        writeManifest(manifest);
    }

    /**
     * Permanently removes a curriculum: deletes the data file from disk and removes the
     * manifest entry entirely. The action is irreversible. A reason and the deleting user
     * are required for audit purposes (logged; the entry itself is gone).
     */
    public void permanentDelete(String id, String reason, String deletedBy) throws IOException {
        ensureBootstrapped();
        List<CurriculumManifestEntry> manifest = readManifest();
        CurriculumManifestEntry target = null;
        for (CurriculumManifestEntry e : manifest) {
            if (id.equals(e.getId())) { target = e; break; }
        }
        if (target == null) {
            throw new IllegalArgumentException("Unknown curriculum id: " + id);
        }
        long active = manifest.stream().filter(e -> !e.isDeleted()).count();
        if (!target.isDeleted() && active <= 1) {
            throw new IllegalArgumentException("Cannot permanently delete the last active curriculum");
        }
        Path dataFile = filesDir().resolve(target.getStorageFile());
        if (Files.exists(dataFile)) {
            Files.delete(dataFile);
        }
        manifest.remove(target);
        writeManifest(manifest);
    }

    public void restore(String id) throws IOException {
        List<CurriculumManifestEntry> manifest = readManifest();
        boolean found = false;
        for (CurriculumManifestEntry e : manifest) {
            if (id.equals(e.getId())) {
                e.setDeleted(false);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown curriculum id: " + id);
        }
        writeManifest(manifest);
    }

    /**
     * Expected: { "page_key": { "tag_key": [ { "qn": 1, "q": "10 + 20" }, ... ], ... }, ... }
     */
    public static void validateCurriculumShape(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Curriculum must be a non-empty JSON object");
        }
        for (Map.Entry<String, Object> page : data.entrySet()) {
            if (!(page.getValue() instanceof Map)) {
                throw new IllegalArgumentException("Page \"" + page.getKey() + "\" must be an object of tags");
            }
            Map<?, ?> tags = (Map<?, ?>) page.getValue();
            for (Map.Entry<?, ?> tag : tags.entrySet()) {
                               if (!(tag.getValue() instanceof List)) {
                    throw new IllegalArgumentException(
                            "Tag \"" + tag.getKey() + "\" under page \"" + page.getKey() + "\" must be an array");
                }
                List<?> rows = (List<?>) tag.getValue();
                for (int i = 0; i < rows.size(); i++) {
                    Object row = rows.get(i);
                    if (!(row instanceof Map)) {
                        throw new IllegalArgumentException("Question " + (i + 1) + " in \"" + tag.getKey()
                                + "\" / \"" + page.getKey() + "\" must be an object with \"q\"");
                    }
                    Map<?, ?> qobj = (Map<?, ?>) row;
                    Object q = qobj.get("q");
                    if (q == null || String.valueOf(q).isBlank()) {
                        throw new IllegalArgumentException("Each question needs a non-empty \"q\" string");
                    }
                }
            }
        }
    }

    static String normalizeOrGenerateId(String requestedId, String displayName) {
        String base = requestedId != null && !requestedId.isBlank()
                ? requestedId
                : (displayName != null ? displayName : "curriculum");
        String s = base.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (s.isBlank()) {
            s = "cur-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (s.length() > 64) {
            s = s.substring(0, 64);
        }
        return s;
    }
}
