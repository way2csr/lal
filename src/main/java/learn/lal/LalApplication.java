package learn.lal;


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
		SpringApplication.run(LalApplication.class, args);
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
