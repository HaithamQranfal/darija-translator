package ma.aui.translator.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Central configuration. Each key is resolved in this order:
 * <ol>
 *   <li>environment variable (e.g. {@code GEMINI_API_KEY})</li>
 *   <li>JVM system property (e.g. {@code -DGEMINI_API_KEY=...})</li>
 *   <li>{@code translator.properties} on the classpath (defaults)</li>
 * </ol>
 * Secrets such as the Gemini API key should only ever be supplied through the environment.
 */
public final class AppConfig {

    public static final String LLM_PROVIDER = "LLM_PROVIDER";
    public static final String GEMINI_API_KEY = "GEMINI_API_KEY";
    public static final String GEMINI_MODEL = "GEMINI_MODEL";
    public static final String GEMINI_BASE_URL = "GEMINI_BASE_URL";
    public static final String OLLAMA_URL = "OLLAMA_URL";
    public static final String OLLAMA_MODEL = "OLLAMA_MODEL";
    public static final String LLM_TIMEOUT_SECONDS = "LLM_TIMEOUT_SECONDS";
    public static final String TRANSLATOR_USERS = "TRANSLATOR_USERS";
    public static final String CACHE_SIZE = "CACHE_SIZE";
    public static final String MAX_TEXT_LENGTH = "MAX_TEXT_LENGTH";

    private static final AppConfig INSTANCE = new AppConfig(loadDefaults());

    private final Properties defaults;

    AppConfig(Properties defaults) {
        this.defaults = defaults;
    }

    public static AppConfig get() {
        return INSTANCE;
    }

    /** Creates a config backed only by the given properties (env/system values still win). Useful for tests. */
    public static AppConfig of(Properties props) {
        return new AppConfig(props);
    }

    public String get(String key, String fallback) {
        String v = System.getenv(key);
        if (isBlank(v)) v = System.getProperty(key);
        if (isBlank(v)) v = defaults.getProperty(key);
        return isBlank(v) ? fallback : v.trim();
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Properties loadDefaults() {
        Properties p = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream("/translator.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {
            // fall back to built-in defaults
        }
        return p;
    }
}
