package learn.lal.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MongoSecretPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Log logger = LogFactory.getLog(MongoSecretPropertySourceEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String syncDir = environment.getProperty("app.git.sync-dir", "data");
        File secretFile = new File(syncDir, "app_secrets.json");

        if (!secretFile.exists()) {
            System.out.println("ℹ️ [MongoSecret] Local secret file not found: " + secretFile.getAbsolutePath());
            return;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> documents = objectMapper.readValue(secretFile, new TypeReference<List<Map<String, Object>>>() {});
            
            Map<String, Object> source = new HashMap<>();
            for (Map<String, Object> doc : documents) {
                Object id = doc.get("_id");
                Object value = doc.get("value");
                if (id != null && value != null) {
                    source.put(id.toString(), value.toString());
                }
            }

            if (!source.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource("mongo-secrets", source));
                System.out.println("✅ [MongoSecret] Loaded " + source.size() + " secret(s) from local JSON file (High Priority).");
            } else {
                System.out.println("ℹ️ [MongoSecret] No secrets found in local JSON file.");
            }
        } catch (Exception e) {
            System.err.println("❌ [MongoSecret] Failed to load secrets from local JSON file: " + e.getMessage());
        }
    }
}
