package ma.aui.translator.llm;

/**
 * Structured answer produced by an LLM for one translation.
 *
 * @param translation      Darija in Arabic script
 * @param transliteration  Darija in Latin script ("Arabizi", e.g. "kifach dayr")
 * @param detectedLanguage language the model detected in the source text
 */
public record LlmResult(String translation, String transliteration, String detectedLanguage) {
}
