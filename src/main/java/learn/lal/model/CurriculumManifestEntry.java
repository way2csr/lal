package learn.lal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One row in curricula/manifest.json. Soft-deleted entries stay in the manifest;
 * when deleted with reason the data file is renamed but kept on disk.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurriculumManifestEntry {

    private String id;
    private String displayName;
    /** Optional label shown in UI, e.g. "Level 2". */
    private String level;
    /** File name under the curricula files directory, e.g. "level2.json". */
    private String storageFile;
    private boolean deleted;

    /** Username of the admin who performed the delete-with-reason action. */
    private String deletedBy;
    /** ISO-8601 timestamp of the delete-with-reason action. */
    private String deletedAt;
    /** Reason entered by the admin when deleting. */
    private String deletionReason;
    /** Original file name before it was renamed on deletion. */
    private String originalStorageFile;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getStorageFile() { return storageFile; }
    public void setStorageFile(String storageFile) { this.storageFile = storageFile; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }

    public String getDeletionReason() { return deletionReason; }
    public void setDeletionReason(String deletionReason) { this.deletionReason = deletionReason; }

    public String getOriginalStorageFile() { return originalStorageFile; }
    public void setOriginalStorageFile(String originalStorageFile) { this.originalStorageFile = originalStorageFile; }
}
