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
public class HuggingFaceController {

    @Value("${spring.ai.huggingface.api-key}")
    private String huggingFaceApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/ai/hf")
    public Map<String, String> generate(@RequestParam(defaultValue = "Tell me a joke") String message) {
        try {
            String url = "https://router.huggingface.co/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(huggingFaceApiKey);

            Map<String, Object> body = Map.of(
                "model", "Qwen/Qwen2.5-72B-Instruct",
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
            
            return Map.of("error", "Empty response from Hugging Face");
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
