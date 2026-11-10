package ma.aui.translator.api.dto;

/** JSON returned by the translate() business method. */
public class TranslationResponse {
    private String original;
    private String translation;
    private String transliteration;
    private String sourceLanguage;
    private String targetLanguage;
    private String provider;
    private String model;
    private boolean cached;
    private long durationMs;

    public String getOriginal() { return original; }
    public void setOriginal(String original) { this.original = original; }
    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }
    public String getTransliteration() { return transliteration; }
    public void setTransliteration(String transliteration) { this.transliteration = transliteration; }
    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}
