package ma.aui.translator.service;

import ma.aui.translator.llm.LlmResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small thread-safe LRU cache so repeated phrases do not consume the LLM's free-tier quota. */
public class TranslationCache {

    private final Map<String, LlmResult> map;
    private final int capacity;

    public TranslationCache(int capacity) {
        this.capacity = Math.max(0, capacity);
        this.map = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, LlmResult> eldest) {
                return size() > TranslationCache.this.capacity;
            }
        });
    }

    public static String key(String text, String sourceLanguage) {
        String src = sourceLanguage == null ? "auto" : sourceLanguage.trim().toLowerCase();
        return src + '\u0000' + text.trim();
    }

    public LlmResult get(String key) {
        return capacity == 0 ? null : map.get(key);
    }

    public void put(String key, LlmResult value) {
        if (capacity > 0) map.put(key, value);
    }

    public int size() {
        return map.size();
    }
}
