package learn.lal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class InfisicalSecretIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void testInfisicalSecretsAreLoaded() {
        // Assert that the mapped Spring AI properties are present
        // Note: We don't assert the actual values for security, just that they are not null or empty
        assertThat(environment.getProperty("spring.ai.openai.api-key"))
                .as("OpenAI API key should be loaded from Infisical and mapped to Spring property")
                .isNotBlank();

        assertThat(environment.getProperty("spring.ai.gemini.api-key"))
                .as("Gemini API key should be loaded from Infisical and mapped to Spring property")
                .isNotBlank();

        assertThat(environment.getProperty("spring.ai.huggingface.api-key"))
                .as("HuggingFace API key should be loaded from Infisical and mapped to Spring property")
                .isNotBlank();
        
        // Assert that raw Infisical keys are also available if needed
        assertThat(environment.getProperty("OPENAI_API_KEY"))
                .as("Raw OPENAI_API_KEY should be present in Environment")
                .isNotBlank();
    }

    @Test
    void testInfisicalPropertySourcePrecedence() {
        // The infisical-secrets source should provide these values.
        // Even if they exist in application.yaml as placeholders like ${OPENAI_API_KEY:},
        // they should now be resolved to actual values.
        String openaiKey = environment.getProperty("spring.ai.openai.api-key");
        assertThat(openaiKey).doesNotContain("${");
    }
}
