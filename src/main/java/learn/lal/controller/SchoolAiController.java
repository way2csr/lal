package learn.lal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/school")
public class SchoolAiController {

    @Value("${spring.ai.gemini.api-key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, String> callGemini(String systemPrompt, String userMessage) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                    Map.of("role", "user",
                           "parts", List.of(Map.of("text", userMessage)))
                ),
                "generationConfig", Map.of(
                    "temperature", 0.7,
                    "maxOutputTokens", 2048
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content =
                        (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts =
                        (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        return Map.of("generation", (String) parts.get(0).get("text"));
                    }
                }
            }
            return Map.of("error", "Empty response from AI");
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 1. AI TUTOR — answers student doubts
    // ──────────────────────────────────────────────
    @GetMapping("/tutor")
    public Map<String, String> tutor(
            @RequestParam String question,
            @RequestParam(defaultValue = "Class 3") String grade,
            @RequestParam(defaultValue = "Maths") String subject,
            @RequestParam(defaultValue = "") String chapter) {

        String system = "You are a friendly, patient maths tutor for " + grade + " students (CBSE, India). "
            + "Subject: " + subject + ". "
            + (chapter.isEmpty() ? "" : "Current chapter: " + chapter + ". ")
            + "Rules:\n"
            + "- Explain in very simple language a 7-8 year old can understand.\n"
            + "- Use Indian context examples (rupees, cricket, festivals, school items).\n"
            + "- Use step-by-step explanations with numbered steps.\n"
            + "- Add a fun fact or encouragement at the end.\n"
            + "- If the question is off-topic, gently redirect to the subject.\n"
            + "- Keep your response under 300 words.\n"
            + "- Use emojis sparingly to keep it friendly.\n"
            + "- Format with simple markdown (bold, bullets, numbered lists).";

        return callGemini(system, question);
    }

    // ──────────────────────────────────────────────
    // 2. AI QUESTION GENERATOR — creates fresh practice questions
    // ──────────────────────────────────────────────
    @GetMapping("/generate-questions")
    public Map<String, String> generateQuestions(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "easy") String difficulty,
            @RequestParam(defaultValue = "Class 3") String grade,
            @RequestParam(defaultValue = "Maths") String subject,
            @RequestParam String topic) {

        String system = "You are an expert CBSE " + grade + " " + subject + " question paper setter. "
            + "Generate exactly " + count + " " + difficulty.toUpperCase() + " level practice questions on the given topic.\n\n"
            + "Rules:\n"
            + "- Difficulty: " + difficulty + " (easy = direct application, medium = requires 2 steps, hard = requires reasoning or multiple concepts).\n"
            + "- Use Indian context (rupees ₹, Indian names, festivals, school items, cricket).\n"
            + "- Return ONLY valid JSON — no markdown fences, no commentary.\n"
            + "- Format: [{\"q\":\"question text\",\"a\":\"answer\",\"hint\":\"one line hint\"}]\n"
            + "- Keep language simple for " + grade + " students.\n"
            + "- Vary question types: fill-in-the-blank, word problems, true/false, compute.";

        return callGemini(system, "Topic: " + topic);
    }

    // ──────────────────────────────────────────────
    // 3. AI HINT — gives a hint without revealing the answer
    // ──────────────────────────────────────────────
    @GetMapping("/hint")
    public Map<String, String> hint(
            @RequestParam String question,
            @RequestParam(defaultValue = "Class 3") String grade,
            @RequestParam(defaultValue = "Maths") String subject) {

        String system = "You are a helpful maths tutor for " + grade + " students. "
            + "The student is stuck on a question and needs a HINT — NOT the full answer.\n\n"
            + "Rules:\n"
            + "- Give exactly ONE short hint (1-2 sentences max).\n"
            + "- The hint should nudge them toward the right approach without revealing the answer.\n"
            + "- Use simple language a 7-8 year old understands.\n"
            + "- Be encouraging (e.g., 'You're close!' or 'Think about...').\n"
            + "- Never give the final answer.";

        return callGemini(system, "I need a hint for this question: " + question);
    }

    // ──────────────────────────────────────────────
    // 4. AI STORY — explains a concept through a fun story
    // ──────────────────────────────────────────────
    @GetMapping("/story")
    public Map<String, String> story(
            @RequestParam String concept,
            @RequestParam(defaultValue = "Class 3") String grade,
            @RequestParam(defaultValue = "Maths") String subject) {

        String system = "You are a creative children's story writer who teaches " + subject + " concepts through short, engaging stories.\n\n"
            + "Rules:\n"
            + "- Write a fun, short story (150-250 words) that teaches the given maths concept.\n"
            + "- Set the story in India — use Indian names, festivals, markets, cricket, school life.\n"
            + "- The characters should be kids around 7-8 years old.\n"
            + "- Weave the maths concept naturally into the story plot.\n"
            + "- End with a 'What did we learn?' sentence.\n"
            + "- Use simple vocabulary appropriate for " + grade + ".\n"
            + "- Make it fun and memorable — use dialogue and action.\n"
            + "- Format with simple markdown.";

        return callGemini(system, "Teach this concept through a story: " + concept);
    }

    // ──────────────────────────────────────────────
    // 5. AI QUIZ GENERATOR — creates fresh MCQs
    // ──────────────────────────────────────────────
    @GetMapping("/generate-quiz")
    public Map<String, String> generateQuiz(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "Class 3") String grade,
            @RequestParam(defaultValue = "Maths") String subject,
            @RequestParam String topic) {

        String system = "You are a CBSE " + grade + " " + subject + " quiz maker.\n"
            + "Generate exactly " + count + " multiple-choice questions on the given topic.\n\n"
            + "Rules:\n"
            + "- Return ONLY valid JSON — no markdown fences, no commentary.\n"
            + "- Format: [{\"q\":\"question\",\"opts\":[\"A\",\"B\",\"C\",\"D\"],\"ans\":0}]\n"
            + "  where 'ans' is the 0-based index of the correct option.\n"
            + "- Use Indian context and simple language for " + grade + " students.\n"
            + "- Make options plausible — avoid obviously silly distractors.\n"
            + "- Cover different aspects of the topic across questions.";

        return callGemini(system, "Topic: " + topic);
    }

    // ──────────────────────────────────────────────
    // 6. AI SIMPLIFIER — re-explains content in simpler terms
    // ──────────────────────────────────────────────
    @GetMapping("/simplify")
    public Map<String, String> simplify(
            @RequestParam String text,
            @RequestParam(defaultValue = "Class 3") String grade,
            @RequestParam(defaultValue = "en") String language) {

        String langName = "en".equals(language) ? "English"
                        : "hi".equals(language) ? "Hindi"
                        : "te".equals(language) ? "Telugu"
                        : "English";

        String system = "You are a teacher who simplifies complex explanations for " + grade + " students.\n\n"
            + "Rules:\n"
            + "- Rewrite the given text in very simple " + langName + " that a 7-8 year old can understand.\n"
            + "- Use short sentences and everyday words.\n"
            + "- Add a simple example if it helps.\n"
            + "- Keep it under 100 words.\n"
            + "- If Hindi is requested, use Devanagari script.\n"
            + "- If Telugu is requested, use Telugu script.";

        return callGemini(system, text);
    }
}
