package learn.lal.config;

import com.infisical.sdk.InfisicalSdk;
import com.infisical.sdk.config.SdkConfig;
import com.infisical.sdk.models.Secret;
import com.infisical.sdk.util.InfisicalException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfisicalInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static {
        try (java.io.FileWriter fw = new java.io.FileWriter("/tmp/infisical_class_load.log", true)) {
            fw.write("InfisicalInitializer class loaded at " + java.time.LocalDateTime.now() + "\n");
        } catch (Exception ignored) {}
    }

    private static final String PROPERTY_SOURCE_NAME = "infisical-secrets";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        String clientId = environment.getProperty("infisical.client-id", "cecff267-619d-40b7-962b-a67d36ba0ab1");
        String clientSecret = environment.getProperty("infisical.client-secret", "01177709c1301220ece223fec672398f1afce3106deac0ae5e31d30d1a9efdd0");
        String projectId = environment.getProperty("infisical.project-id", "397abbdb-c673-4d6e-8d8e-ed0bc928f2d6");
        String envSlug = environment.getProperty("infisical.environment", "dev");
        String siteUrl = environment.getProperty("infisical.site-url", "https://app.infisical.com");

        System.err.println("🔐 [Infisical] Initializing secrets from " + siteUrl + " (project=" + projectId + ", env=" + envSlug + ")");
        try (java.io.FileWriter fw = new java.io.FileWriter("/tmp/infisical_init.log", true)) {
            fw.write("Initializing at " + java.time.LocalDateTime.now() + "\n");
            fw.write("Project: " + projectId + "\n");

            InfisicalSdk sdk = new InfisicalSdk(new SdkConfig.Builder()
                    .withSiteUrl(siteUrl)
                    .build());

            sdk.Auth().UniversalAuthLogin(clientId, clientSecret);
            fw.write("Auth successful\n");

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
                    secretsMap.put(secret.getSecretKey(), secret.getSecretValue());
                    fw.write("Loaded: " + secret.getSecretKey() + "\n");
                }
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, secretsMap));
                System.err.println("✅ [Infisical] Successfully loaded " + secretsMap.size() + " secret(s).");
                fw.write("Success: loaded " + secretsMap.size() + " secrets\n");
            } else {
                System.err.println("ℹ️ [Infisical] No secrets found in path /");
                fw.write("No secrets found\n");
            }

        } catch (InfisicalException e) {
            System.err.println("❌ [Infisical] Failed to load secrets: " + e.getMessage());
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("/tmp/infisical_init.log", true))) {
                pw.println("InfisicalException: " + e.getMessage());
                e.printStackTrace(pw);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            System.err.println("❌ [Infisical] Unexpected error during secret loading: " + e.getMessage());
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("/tmp/infisical_init.log", true))) {
                pw.println("Exception: " + e.getMessage());
                e.printStackTrace(pw);
            } catch (Exception ignored) {}
        }
    }
}
