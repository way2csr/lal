package learn.lal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
public class GrokController {

    @Value("${spring.ai.grok.api-key}")
    private String grokApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/ai/grok")
    public Map<String, String> generate(@RequestParam(defaultValue = "Tell me a joke") String message) {
        try {
            String url = "https://api.x.ai/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(grokApiKey);

            Map<String, Object> body = Map.of(
                "model", "grok-2-latest",
                "messages", List.of(
                    Map.of("role", "user", "content", message)
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> messageResponse = (Map<String, Object>) choices.get(0).get("message");
                    String generatedText = (String) messageResponse.get("content");
                    return Map.of("generation", generatedText);
                }
            }
            
            return Map.of("error", "Empty response from Grok");
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
