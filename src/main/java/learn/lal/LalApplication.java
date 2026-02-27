package learn.lal;

import learn.lal.config.InfisicalInitializer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class LalApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(LalApplication.class);

		// Load Infisical secrets before Spring context starts.
		// Credentials come from environment variables (set in shell or CI/CD).
		// For local dev, export these in your shell profile or .env file.
		String clientId     = System.getenv("INFISICAL_CLIENT_ID");
		String clientSecret = System.getenv("INFISICAL_CLIENT_SECRET");
		String projectId    = System.getenv().getOrDefault("INFISICAL_PROJECT_ID", "397abbdb-c673-4d6e-8d8e-ed0bc928f2d6");
		String envSlug      = System.getenv().getOrDefault("INFISICAL_ENV", "dev");
		String siteUrl      = System.getenv().getOrDefault("INFISICAL_SITE_URL", "https://app.infisical.com");

		if (clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank()) {
			try {
				System.out.println("🔐 [Infisical] Fetching secrets for project: " + projectId);
				com.infisical.sdk.InfisicalSdk sdk = new com.infisical.sdk.InfisicalSdk(
					new com.infisical.sdk.config.SdkConfig.Builder().withSiteUrl(siteUrl).build()
				);
				sdk.Auth().UniversalAuthLogin(clientId, clientSecret);

				java.util.List<com.infisical.sdk.models.Secret> secrets = sdk.Secrets().ListSecrets(
					projectId, envSlug, "/", true, false, true
				);

				if (secrets != null && !secrets.isEmpty()) {
					java.util.Map<String, Object> secretsMap = new java.util.HashMap<>();
					for (com.infisical.sdk.models.Secret s : secrets) {
						secretsMap.put(s.getSecretKey(), s.getSecretValue());
					}
					app.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(
						new org.springframework.core.env.MapPropertySource("infisical-secrets", secretsMap)
					));
					System.out.println("✅ [Infisical] Loaded " + secretsMap.size() + " secret(s).");
				} else {
					System.out.println("ℹ️  [Infisical] No secrets found in path /");
				}
			} catch (Exception e) {
				System.err.println("❌ [Infisical] Failed to load secrets: " + e.getMessage());
			}
		} else {
			System.out.println("ℹ️  [Infisical] INFISICAL_CLIENT_ID / INFISICAL_CLIENT_SECRET not set — skipping.");
		}

		app.run(args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void openBrowser() {
		String url = "http://localhost:8080/learn.html";
		System.setProperty("java.awt.headless", "false");
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(new URI(url));
			} else {
				String os = System.getProperty("os.name").toLowerCase();
				Runtime rt = Runtime.getRuntime();
				if (os.contains("win")) {
					rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
				} else if (os.contains("mac")) {
					rt.exec("open " + url);
				} else if (os.contains("nix") || os.contains("nux")) {
					String[] browsers = { "xdg-open", "google-chrome", "firefox", "opera", "konqueror", "epiphany",
							"mozilla", "netscape" };
					String browser = null;
					for (String b : browsers) {
						if (rt.exec(new String[] { "which", b }).getInputStream().read() != -1) {
							browser = b;
							break;
						}
					}
					if (browser != null) {
						rt.exec(new String[] { browser, url });
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Note: Could not automatically open browser. Please navigate to: " + url);
		}
	}
}
