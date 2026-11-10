package ma.aui.translator.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ma.aui.translator.api.dto.TranslationResponse;
import ma.aui.translator.config.AppConfig;
import ma.aui.translator.llm.LlmClient;
import ma.aui.translator.llm.LlmResult;

/** Business logic: validates input, builds prompts, calls the LLM and caches results. */
@ApplicationScoped
public class TranslationService {

    public static final String TARGET_LANGUAGE = "Moroccan Arabic (Darija)";

    private LlmClient llm;
    private PromptBuilder prompts;
    private TranslationCache cache;
    private int maxLength;

    /** Required by CDI for proxying. */
    protected TranslationService() {
    }

    @Inject
    public TranslationService(LlmClient llm) {
        this(llm, new PromptBuilder(),
                new TranslationCache(AppConfig.get().getInt(AppConfig.CACHE_SIZE, 500)),
                AppConfig.get().getInt(AppConfig.MAX_TEXT_LENGTH, 5000));
    }

    public TranslationService(LlmClient llm, PromptBuilder prompts, TranslationCache cache, int maxLength) {
        this.llm = llm;
        this.prompts = prompts;
        this.cache = cache;
        this.maxLength = maxLength;
    }

    public TranslationResponse translate(String text, String sourceLanguage) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("'text' must not be empty");
        }
        if (text.length() > maxLength) {
            throw new IllegalArgumentException("'text' is too long (" + text.length() + " > " + maxLength + " characters)");
        }
        long start = System.nanoTime();
        String key = TranslationCache.key(text, sourceLanguage);
        LlmResult result = cache.get(key);
        boolean cached = result != null;
        if (!cached) {
            result = llm.complete(prompts.systemPrompt(), prompts.userPrompt(text.trim(), sourceLanguage));
            cache.put(key, result);
        }
        TranslationResponse r = new TranslationResponse();
        r.setOriginal(text);
        r.setTranslation(result.translation());
        r.setTransliteration(result.transliteration());
        r.setSourceLanguage(result.detectedLanguage() != null ? result.detectedLanguage()
                : (sourceLanguage == null ? "auto" : sourceLanguage));
        r.setTargetLanguage(TARGET_LANGUAGE);
        r.setProvider(llm.providerName());
        r.setModel(llm.modelName());
        r.setCached(cached);
        r.setDurationMs((System.nanoTime() - start) / 1_000_000);
        return r;
    }

    public String providerName() {
        return llm.providerName();
    }

    public String modelName() {
        return llm.modelName();
    }
}
