package ma.aui.translator.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import ma.aui.translator.config.AppConfig;

import java.time.Duration;
import java.util.Locale;
import java.util.logging.Logger;

/** CDI producer that picks the {@link LlmClient} implementation from {@code LLM_PROVIDER}. */
@ApplicationScoped
public class LlmClientFactory {

    private static final Logger LOG = Logger.getLogger(LlmClientFactory.class.getName());

    @Produces
    @ApplicationScoped
    public LlmClient produce() {
        return create(AppConfig.get());
    }

    public static LlmClient create(AppConfig cfg) {
        String provider = cfg.get(AppConfig.LLM_PROVIDER, "gemini").toLowerCase(Locale.ROOT);
        Duration timeout = Duration.ofSeconds(cfg.getInt(AppConfig.LLM_TIMEOUT_SECONDS, 60));
        LlmClient client = switch (provider) {
            case "ollama" -> new OllamaClient(
                    cfg.get(AppConfig.OLLAMA_URL, "http://localhost:11434"),
                    cfg.get(AppConfig.OLLAMA_MODEL, "qwen2.5:3b"),
                    timeout);
            case "mock" -> new MockLlmClient();
            case "gemini" -> new GeminiClient(
                    cfg.get(AppConfig.GEMINI_API_KEY, null),
                    cfg.get(AppConfig.GEMINI_MODEL, "gemini-flash-latest"),
                    cfg.get(AppConfig.GEMINI_BASE_URL, "https://generativelanguage.googleapis.com/v1beta"),
                    timeout);
            default -> throw new IllegalStateException("Unknown LLM_PROVIDER '" + provider + "' (use gemini, ollama or mock)");
        };
        LOG.info(() -> "Translator LLM provider: " + client.providerName() + " / model " + client.modelName());
        return client;
    }
}
