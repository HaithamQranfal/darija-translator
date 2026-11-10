package ma.aui.translator.llm;

import java.util.Locale;
import java.util.Map;

/**
 * Offline stand-in used for tests and for demoing the clients without an API key.
 * It only knows a handful of phrases; anything else is returned with a "[mock]" marker.
 */
public class MockLlmClient implements LlmClient {

    private static final Map<String, String[]> PHRASES = Map.of(
            "hello", new String[]{"السلام", "salam"},
            "how are you?", new String[]{"كي داير؟", "kidayr?"},
            "thank you", new String[]{"شكرا بزاف", "choukran bzaf"},
            "good morning", new String[]{"صباح الخير", "sbah lkhir"},
            "i love morocco", new String[]{"كنبغي المغرب", "kanbghi lmghrib"},
            "where is the train station?", new String[]{"فين كاينة لاگار؟", "fin kayna lagar?"});

    @Override
    public LlmResult complete(String systemPrompt, String userPrompt) {
        String text = PromptMarkers.extract(userPrompt).trim();
        String[] hit = PHRASES.get(text.toLowerCase(Locale.ROOT));
        if (hit != null) {
            return new LlmResult(hit[0], hit[1], "English");
        }
        return new LlmResult("[mock] " + text, null, "unknown");
    }

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public String modelName() {
        return "mock-dictionary";
    }
}
