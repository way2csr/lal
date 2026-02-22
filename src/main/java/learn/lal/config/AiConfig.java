package learn.lal.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.huggingface.api-key}")
    private String huggingFaceApiKey;

    @Value("${spring.ai.gemini.api-key}")
    private String geminiApiKey;

    @Bean
    @Primary
    public ChatModel openAiChatModel() {
        var openAiApi = OpenAiApi.builder()
                .apiKey(openAiApiKey)
                .build();
        var options = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    public ChatModel huggingFaceChatModel() {
        var openAiApi = OpenAiApi.builder()
                .baseUrl("https://router.huggingface.co/v1/")
                .apiKey(huggingFaceApiKey)
                .build();
        var options = OpenAiChatOptions.builder()
                .model("Qwen/Qwen2.5-72B-Instruct")
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
    
    @Bean
    public ChatModel geminiChatModel() {
        var openAiApi = OpenAiApi.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/")
                .apiKey(geminiApiKey)
                .build();
        var options = OpenAiChatOptions.builder()
                .model("gemini-1.5-flash")
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
