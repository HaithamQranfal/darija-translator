package ma.aui.translator.llm;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import java.io.StringReader;

/** Turns the JSON text produced by the model into an {@link LlmResult}, tolerating code fences and plain text. */
final class LlmResponseParser {

    private LlmResponseParser() {
    }

    static LlmResult parse(String modelText) {
        if (modelText == null || modelText.isBlank()) {
            throw new LlmException("The LLM returned an empty answer");
        }
        String cleaned = stripCodeFences(modelText.trim());
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try (JsonReader reader = Json.createReader(new StringReader(cleaned.substring(start, end + 1)))) {
                JsonObject o = reader.readObject();
                String translation = str(o, "translation");
                if (translation != null && !translation.isBlank()) {
                    return new LlmResult(translation.trim(), str(o, "transliteration"), str(o, "detectedLanguage"));
                }
            } catch (RuntimeException ignored) {
                // not valid JSON: fall through and use the raw text
            }
        }
        // The model ignored the JSON instruction: treat the whole answer as the translation.
        return new LlmResult(cleaned, null, null);
    }

    private static String str(JsonObject o, String key) {
        JsonValue v = o.get(key);
        if (v == null || v.getValueType() == JsonValue.ValueType.NULL) return null;
        return v.getValueType() == JsonValue.ValueType.STRING ? o.getString(key) : v.toString();
    }

    private static String stripCodeFences(String s) {
        if (s.startsWith("```")) {
            int firstNewLine = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewLine > 0 && lastFence > firstNewLine) {
                return s.substring(firstNewLine + 1, lastFence).trim();
            }
        }
        return s;
    }
}
