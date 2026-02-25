package learn.lal.controller;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secrets")
public class ConfigController {

    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping
    public ResponseEntity<Map<String, String>> getAllSecrets() {
        Map<String, String> secrets = new HashMap<>();
        if (mongoTemplate.collectionExists("app_secrets")) {
            List<Document> docs = mongoTemplate.findAll(Document.class, "app_secrets");
            for (Document doc : docs) {
                Object id = doc.get("_id");
                Object value = doc.get("value");
                if (id != null && value != null) {
                    secrets.put(id.toString(), value.toString());
                }
            }
        }
        return ResponseEntity.ok(secrets);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> saveSecrets(@RequestBody Map<String, String> updatedSecrets) {
        log.info("Received request to update {} secrets from UI", updatedSecrets.size());
        
        if (!mongoTemplate.collectionExists("app_secrets")) {
            mongoTemplate.createCollection("app_secrets");
        }

        int updatedCount = 0;
        for (Map.Entry<String, String> entry : updatedSecrets.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                // Upsert operation in MongoDB
                org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(entry.getKey())
                );
                org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                        .set("value", entry.getValue());
                mongoTemplate.upsert(query, update, "app_secrets");
                updatedCount++;
            }
        }

        log.info("Successfully updated {} secrets in MongoDB", updatedCount);

        // Touch application-local.yaml to trigger DevTools restart
        triggerRestart();

        return ResponseEntity.ok(Map.of("message", "Secrets updated successfully. Restarting server..."));
    }

    private void triggerRestart() {
        new Thread(() -> {
            try {
                // Wait briefly for the response to reach the frontend natively
                Thread.sleep(1000);
                Path fileToTouch = Paths.get("src/main/resources/application-local.yaml");
                if (Files.exists(fileToTouch)) {
                    log.info("Touching application-local.yaml to trigger DevTools restart...");
                    Files.setLastModifiedTime(fileToTouch, FileTime.from(Instant.now()));
                } else {
                    fileToTouch = Paths.get("src/main/resources/application.yaml");
                    if (Files.exists(fileToTouch)) {
                        log.info("Touching application.yaml to trigger DevTools restart...");
                        Files.setLastModifiedTime(fileToTouch, FileTime.from(Instant.now()));
                    }
                }
            } catch (InterruptedException | IOException e) {
                log.error("Failed to trigger application restart", e);
            }
        }).start();
    }
}
