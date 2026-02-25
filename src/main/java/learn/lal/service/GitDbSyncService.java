package learn.lal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GitDbSyncService {

    private static final Logger log = LoggerFactory.getLogger(GitDbSyncService.class);

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final String syncDir;

    public GitDbSyncService(MongoTemplate mongoTemplate,
                            ObjectMapper objectMapper,
                            @Value("${app.git.sync-dir:data}") String syncDir) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.syncDir = syncDir;
    }

    @PostConstruct
    public void onStartup() {
        log.info("Starting Git DB Sync...");
        
        // 1. Git Pull (Optional depending on how you want to handle remote changes)
        // Ignoring remote pull for now as this is a local development instance 
        // that's pushing to the same repository. We'll just read local files.

        // 2. Ensure sync directory exists
        File dir = new File(syncDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 3. Read JSON files and load into Embedded MongoDB
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String collectionName = file.getName().replace(".json", "");
                try {
                    List<Map<String, Object>> documents = objectMapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
                    log.info("Loading {} documents into collection: {}", documents.size(), collectionName);
                    
                    // Clear existing if any (usually embedded is empty on startup, but just in case)
                    if (mongoTemplate.collectionExists(collectionName)) {
                         mongoTemplate.dropCollection(collectionName);
                    }
                    
                    for (Map<String, Object> doc : documents) {
                        mongoTemplate.insert(doc, collectionName);
                    }
                    log.info("Successfully loaded collection: {}", collectionName);
                } catch (Exception e) {
                    log.error("Failed to load collection data from file: {}", file.getName(), e);
                }
            }
        } else {
             log.info("No JSON database files found in directory: {}", syncDir);
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Shutting down... Executing Git DB Sync Export...");

        // 1. Export all collections to JSON files
        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        File dir = new File(syncDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (String collectionName : collectionNames) {
            // Skip system or internal collections if necessary, but embedded usually just has what we created
            exportCollection(collectionName);
        }

        // 2. Commit and Push
        executeGitPush();
    }

    private void exportCollection(String collectionName) {
        try {
            List<Map> allDocuments = mongoTemplate.findAll(Map.class, collectionName);
            Path filePath = Paths.get(syncDir, collectionName + ".json");
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), allDocuments);
            log.info("Exported {} documents from collection {} to {}", allDocuments.size(), collectionName, filePath);
            
        } catch (Exception e) {
            log.error("Failed to export collection: {}", collectionName, e);
        }
    }

    private void executeGitPush() {
        try {
            log.info("Executing Git Push for database changes...");
            // We use ProcessBuilder to run git commands
            runCommand(new String[]{"git", "add", syncDir + "/*"}, "Git Add");
            
            // Only commit if there are changes
            int commitExitStatus = runCommandReturnStatus(new String[]{"git", "commit", "-m", "Auto-save embedded DB state " + System.currentTimeMillis()}, "Git Commit");
            
            if (commitExitStatus == 0) {
                 log.info("Changes committed. Pushing to remote...");
                 runCommand(new String[]{"git", "push"}, "Git Push");
            } else {
                 log.info("No database changes to commit or commit failed.");
            }
            
        } catch (Exception e) {
            log.error("Failed to execute Git push for database sync", e);
        }
    }

    private int runCommandReturnStatus(String[] command, String name) {
         try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(".")); // Project root
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                 log.warn("{} failed with exit code: {}", name, exitCode);
            }
            return exitCode;
        } catch (Exception e) {
            log.warn("Exception running command: {}", name, e);
            return -1;
        }
    }

    private void runCommand(String[] command, String name) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        // Setting directory to root workspace (where .git is)
        pb.directory(new File("."));
        Process process = pb.start();
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("{} finished with non-zero exit code: {}", name, exitCode);
        } else {
            log.info("{} completed successfully.", name);
        }
    }
}
