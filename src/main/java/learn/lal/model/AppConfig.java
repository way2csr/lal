package learn.lal.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "app_config")
public class AppConfig {

    @Id
    private String key;

    private Map<String, Object> value;

    private String updatedBy;

    private Instant updatedAt;

    public AppConfig() {}

    public AppConfig(String key, Map<String, Object> value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public Map<String, Object> getValue() { return value; }
    public void setValue(Map<String, Object> value) { this.value = value; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
