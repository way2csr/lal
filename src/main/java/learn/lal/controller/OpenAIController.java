package learn.lal.controller;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OpenAIController {

    private final ChatModel chatModel;

    public OpenAIController(
            @org.springframework.beans.factory.annotation.Qualifier("openAiChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/ai/generate")
    public Map<String, String> generate(@RequestParam(defaultValue = "hi") String message) {
        return Map.of("generation", chatModel.call(message));
    }
}
