package learn.lal.config;

import com.infisical.sdk.InfisicalSdk;
import com.infisical.sdk.config.SdkConfig;
import com.infisical.sdk.models.Secret;
import com.infisical.sdk.util.InfisicalException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfisicalEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static {
        System.err.println("🚀 [Infisical] InfisicalEnvironmentPostProcessor class loaded!");
    }

    private static final String PROPERTY_SOURCE_NAME = "infisical-secrets";
    private static final String LOG_FILE = System.getProperty("java.io.tmpdir") + java.io.File.separator + "infisical_processor.log";

    private static final Map<String, String> KEY_MAPPING = new HashMap<>();
    static {
        KEY_MAPPING.put("OPENAI_API_KEY", "spring.ai.openai.api-key");
        KEY_MAPPING.put("HF_API_KEY", "spring.ai.huggingface.api-key");
        KEY_MAPPING.put("GEMINI_API_KEY", "spring.ai.gemini.api-key");
        KEY_MAPPING.put("GROK_API_KEY", "spring.ai.grok.api-key");
        KEY_MAPPING.put("SARVAM_API_KEY", "spring.ai.sarvam.api-key");
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // We want to run after application-local.yaml is loaded.
        // Spring Boot's ConfigDataEnvironmentPostProcessor loads application.yaml files.
        // It has Order = Ordered.LOWEST_PRECEDENCE - 10.
        // We will use a lower order value (higher precedence in terms of execution order for processors, 
        // but wait... EPPs are executed in order. We want to run AFTER the one that loads files.)
        // Actually, many built-in ones run early.
        
        String clientId = environment.getProperty("infisical.client-id");
        String clientSecret = environment.getProperty("infisical.client-secret");
        String projectId = environment.getProperty("infisical.project-id");
        String envSlug = environment.getProperty("infisical.environment", "dev");
        String siteUrl = environment.getProperty("infisical.site-url", "https://app.infisical.com");

        System.err.println("🔍 [Infisical] Processor running. ClientId present: " + (clientId != null && !clientId.isBlank()));

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log("ℹ️ [Infisical] Credentials not found in environment — skipping secret fetch.");
            return;
        }

        log("🔐 [Infisical] Initializing secrets (project=" + projectId + ", env=" + envSlug + ")");

        try {
            InfisicalSdk sdk = new InfisicalSdk(new SdkConfig.Builder().withSiteUrl(siteUrl).build());
            sdk.Auth().UniversalAuthLogin(clientId, clientSecret);
            log("Auth successful");

            List<Secret> secrets = sdk.Secrets().ListSecrets(
                    projectId,
                    envSlug,
                    "/",
                    true,
                    false,
                    true
            );

            if (secrets != null && !secrets.isEmpty()) {
                Map<String, Object> secretsMap = new HashMap<>();
                for (Secret secret : secrets) {
                    String key = secret.getSecretKey();
                    String value = secret.getSecretValue();
                    secretsMap.put(key, value);
                    
                    // Also map to Spring-specific properties if defined
                    if (KEY_MAPPING.containsKey(key)) {
                        secretsMap.put(KEY_MAPPING.get(key), value);
                        log("Mapped: " + key + " -> " + KEY_MAPPING.get(key));
                    }
                    
                    log("Fetched: " + key);
                }
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, secretsMap));
                System.out.println("✅ [Infisical] Successfully loaded " + secretsMap.size() + " secret(s) into Environment.");
                log("Success: loaded " + secretsMap.size() + " secrets");
            } else {
                System.out.println("ℹ️ [Infisical] No secrets found in path /");
                log("No secrets found");
            }

        } catch (InfisicalException e) {
            System.err.println("❌ [Infisical] Failed to load secrets (InfisicalException): " + e.getMessage());
            logError("InfisicalException", e);
        } catch (Exception e) {
            System.err.println("❌ [Infisical] Unexpected error during secret loading: " + e.getMessage());
            logError("Exception", e);
        }
    }

    @Override
    public int getOrder() {
        // Run after ConfigDataEnvironmentPostProcessor (which is LOWEST_PRECEDENCE - 10)
        // so that application-local.yaml properties are available.
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void log(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write("[" + LocalDateTime.now() + "] " + message + "\n");
        } catch (Exception ignored) {}
    }

    private void logError(String type, Exception e) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println("[" + LocalDateTime.now() + "] " + type + ": " + e.getMessage());
            e.printStackTrace(pw);
        } catch (Exception ignored) {}
    }
}
