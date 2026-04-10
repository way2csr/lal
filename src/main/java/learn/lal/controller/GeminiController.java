package learn.lal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
public class GeminiController {

    @Value("${spring.ai.gemini.api-key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/ai/gemini")
    public Map<String, String> generate(@RequestParam(defaultValue = "Tell me a joke") String message) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Constructing Gemini API request body manually
            Map<String, Object> body = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", message)
                    ))
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            // Parsing the Gemini REST API response manually
            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        String generatedText = (String) parts.get(0).get("text");
                        return Map.of("generation", generatedText);
                    }
                }
            }
            
            return Map.of("error", "Empty response from Gemini");
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/ai/gemini/vision")
    public Map<String, String> vision(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return Map.of("error", "No file uploaded");

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

            byte[] fileContent = file.getBytes();
            String base64Content = java.util.Base64.getEncoder().encodeToString(fileContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String prompt = "Extract all mathematical arithmetic questions from this image. " +
                    "Return the result as a valid JSON array of objects, starting with [ and ending with ]. " +
                    "Each object must have 'expr' (the math expression like '12 + 5') and 'answer' (the result as a number). " +
                    "If a question has multiple numbers stacked vertically, extract them as a single expression. " +
                    "Keep the original order from the image. Output ONLY the JSON array string.";

            Map<String, Object> body = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt),
                        Map.of("inline_data", Map.of(
                            "mime_type", file.getContentType(),
                            "data", base64Content
                        ))
                    ))
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        String generatedText = (String) parts.get(0).get("text");
                        return Map.of("generation", generatedText);
                    }
                }
            }
            
            return Map.of("error", "Empty response from Gemini");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }
}
