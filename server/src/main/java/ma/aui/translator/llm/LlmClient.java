package ma.aui.translator.llm;

/**
 * Strategy interface for any Large Language Model able to perform the translation.
 * Implementations: {@link GeminiClient} (cloud, free tier), {@link OllamaClient} (lightweight local LLM)
 * and {@link MockLlmClient} (offline demo / tests).
 */
public interface LlmClient {

    /**
     * Sends the prompts to the model and returns the parsed result.
     *
     * @param systemPrompt instructions that define the translator's behaviour
     * @param userPrompt   the text to translate (already wrapped by {@code PromptBuilder})
     */
    LlmResult complete(String systemPrompt, String userPrompt) throws LlmException;

    /** Short provider id, e.g. "gemini". */
    String providerName();

    /** Model identifier used by the provider, e.g. "gemini-flash-latest". */
    String modelName();
}
