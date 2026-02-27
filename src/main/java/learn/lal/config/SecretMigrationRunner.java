package learn.lal.config;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;

import org.springframework.core.env.Environment;

@Configuration
public class SecretMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecretMigrationRunner.class);

    @Bean
    public CommandLineRunner migrateSecrets(MongoTemplate mongoTemplate, Environment env) {
        return args -> {
            // --- Infisical diagnostic ---
            String clientId = env.getProperty("infisical.client-id", "<not set>");
            log.info("=== Infisical Diagnostic ===");
            log.info("infisical.client-id = {}", clientId.isEmpty() ? "<empty>" : clientId.substring(0, 8) + "...");
            log.info("Property sources: {}",
                ((org.springframework.core.env.ConfigurableEnvironment) env).getPropertySources().stream()
                        .map(ps -> ps.getName()).collect(java.util.stream.Collectors.joining(", ")));
            String openAiKey = env.getProperty("spring.ai.openai.api-key", "<empty>");
            log.info("spring.ai.openai.api-key = {}", openAiKey.isEmpty() ? "<empty>" :
                    openAiKey.substring(0, Math.min(12, openAiKey.length())) + "...");
            log.info("=== End Infisical Diagnostic ===");
            // --- end diagnostic ---

            boolean collectionExists = mongoTemplate.collectionExists("app_secrets");

            long count = collectionExists ? mongoTemplate.getCollection("app_secrets").countDocuments() : 0;
            
            if (count == 0) {
                log.info("Migrating secrets to MongoDB app_secrets collection...");
                String[] keysToMigrate = {
                    "spring.ai.openai.api-key",
                    "spring.ai.huggingface.api-key",
                    "spring.ai.gemini.api-key",
                    "spring.ai.sarvam.api-key",
                    "spring.ai.gra.api-key",
                    "spring.ai.grok.api-key",
                    "app.users.nirvaan.password",
                    "app.users.devaansh.password",
                    "app.users.admin.password"
                };

                int migratedCount = 0;
                for (String key : keysToMigrate) {
                    String value = env.getProperty(key);
                    if (value != null && !value.isBlank()) {
                        Document doc = new Document("_id", key).append("value", value);
                        mongoTemplate.getCollection("app_secrets").insertOne(doc);
                        migratedCount++;
                    }
                }
                log.info("Migrated {} secrets to MongoDB from application properties.", migratedCount);
            } else {
                log.info("Secrets already exist in MongoDB, skipping migration.");
            }
        };
    }
}
