package learn.lal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend proxy for Azure Cognitive Services – Text Translation API v3.
 * Keeps the API key server-side; the browser never sees it.
 *
 * Endpoint:  GET /ai/microsoft-translate?text=Hello&to=hi&to=te
 * Response:  { "generation": "{\"hi\":\"नमस्ते\",\"te\":\"హలో\"}" }
 *        or  { "error": "..." }
 *
 * Configure via env-var AZURE_TRANSLATOR_KEY (or application-local.yaml).
 */
@RestController
@RequestMapping("/ai/microsoft-translate")
public class MicrosoftTranslatorController {

    private static final String TRANSLATE_URL =
            "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0";

    @Value("${azure.translator.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public Map<String, Object> translate(
            @RequestParam String text,
            @RequestParam List<String> to) {

        if (apiKey == null || apiKey.isBlank()) {
            return Map.of("error", "Microsoft Translator API key is not configured (AZURE_TRANSLATOR_KEY).");
        }
        if (text == null || text.isBlank()) {
            return Map.of("error", "text parameter is required.");
        }
        if (to == null || to.isEmpty()) {
            return Map.of("error", "At least one 'to' language parameter is required.");
        }

        try {
            // Build URL with each target language appended
            StringBuilder urlBuilder = new StringBuilder(TRANSLATE_URL);
            for (String lang : to) {
                urlBuilder.append("&to=").append(lang);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Ocp-Apim-Subscription-Key", apiKey);

            // Microsoft expects a JSON array of objects with a "Text" key
            List<Map<String, String>> requestBody = List.of(Map.of("Text", text));
            HttpEntity<List<Map<String, String>>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<List> response = restTemplate.postForEntity(urlBuilder.toString(), entity, List.class);
            List<Map<String, Object>> body = response.getBody();

            if (body == null || body.isEmpty()) {
                return Map.of("error", "Empty response from Microsoft Translator.");
            }

            // Extract translations array from the first (and only) result item
            @SuppressWarnings("unchecked")
            List<Map<String, String>> translations =
                    (List<Map<String, String>>) body.get(0).get("translations");

            if (translations == null) {
                return Map.of("error", "Unexpected response shape from Microsoft Translator.");
            }

            // Build { langCode -> translatedText } map preserving insertion order
            Map<String, String> result = new LinkedHashMap<>();
            for (Map<String, String> t : translations) {
                result.put(t.get("to"), t.get("text"));
            }

            // Return in the same unified format as all other AI endpoints:
            // { "generation": "<JSON string>" }
            return Map.of("generation", objectMapper.writeValueAsString(result));

        } catch (Exception e) {
            return Map.of("error", "Microsoft Translator request failed: " + e.getMessage());
        }
    }
}
