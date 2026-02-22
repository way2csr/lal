package learn.lal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/sarvam")
public class SarvamController {

    @Value("${spring.ai.sarvam.api-key}")
    private String sarvamApiKey;

    private final RestTemplate restTemplate;

    public SarvamController() {
        this.restTemplate = new RestTemplate();
    }

    @GetMapping
    public Map<String, String> generate(@RequestParam(value = "message") String message) {
        String url = "https://api.sarvam.ai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sarvamApiKey);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "sarvam-m");
        request.put("messages", List.of(
            Map.of("role", "system", "content",
                "You are a JSON-only assistant. Rules you must NEVER break:\n" +
                "1. Output ONLY a valid JSON object. No markdown, no ``` fences, no explanation text.\n" +
                "2. Every JSON string value must be a single line. No literal newlines inside string values.\n" +
                "3. Use \\n escape sequences if line breaks are needed inside a string.\n" +
                "4. Do not add trailing commas.\n" +
                "5. All quotes inside strings must be escaped as \\\".\n" +
                "Example good output: {\"translations\":{\"hi\":\"नमस्ते\",\"te\":\"హలో\"},\"vocabulary\":[{\"en\":\"hello\",\"hi\":\"नमस्ते\",\"te\":\"హలో\"}]}"),
            Map.of("role", "user", "content", message)
        ));
        request.put("temperature", 0.05);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty() && choices.get(0).containsKey("message")) {
                    Map<String, Object> messageContent = (Map<String, Object>) choices.get(0).get("message");
                    return Map.of("generation", (String) messageContent.get("content"));
                }
            }
            return Map.of("error", "No valid choices returned from Sarvam AI.");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Sarvam AI failed: " + e.getMessage());
        }
    }
}
