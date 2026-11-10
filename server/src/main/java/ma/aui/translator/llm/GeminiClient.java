package ma.aui.translator.llm;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Calls the Google Gemini API ({@code models/{model}:generateContent}).
 * Get a free API key at https://aistudio.google.com/apikey and export it as {@code GEMINI_API_KEY}.
 */
public class GeminiClient implements LlmClient {

    static final int MAX_RETRIES = 2;
    static final long RETRY_BACKOFF_MS = 1000;

    private final HttpClient http;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;

    public GeminiClient(String apiKey, String model, String baseUrl, Duration timeout) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public LlmResult complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException("GEMINI_API_KEY is not set on the server. Export it, or start the server with LLM_PROVIDER=ollama or mock.");
        }
        JsonObject body = Json.createObjectBuilder()
                .add("systemInstruction", Json.createObjectBuilder()
                        .add("parts", Json.createArrayBuilder().add(Json.createObjectBuilder().add("text", systemPrompt))))
                .add("contents", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("role", "user")
                        .add("parts", Json.createArrayBuilder().add(Json.createObjectBuilder().add("text", userPrompt)))))
                .add("generationConfig", Json.createObjectBuilder()
                        .add("temperature", 0.2)
                        .add("responseMimeType", "application/json"))
                .build();

        String url = baseUrl + "/models/" + URLEncoder.encode(model, StandardCharsets.UTF_8) + ":generateContent";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        int attempt = 0;
        while (true) {
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                // 500/503 ("model is overloaded") are transient on Gemini: retry with a short backoff.
                if (!isTransient(response.statusCode()) || attempt >= MAX_RETRIES) break;
                Thread.sleep(RETRY_BACKOFF_MS * (1L << attempt++));
            } catch (IOException e) {
                throw new LlmException("Cannot reach Gemini: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LlmException("Interrupted while calling Gemini", e);
            }
        }

        if (response.statusCode() != 200) {
            throw new LlmException("Gemini returned HTTP " + response.statusCode() + ": "
                    + abbreviate(response.body()), response.statusCode(), null);
        }
        return LlmResponseParser.parse(extractText(response.body()));
    }

    /** Extracts candidates[0].content.parts[*].text from a generateContent response. */
    static String extractText(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject root = reader.readObject();
            JsonArray candidates = root.getJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new LlmException("Gemini returned no candidates (prompt may have been blocked): " + abbreviate(json));
            }
            JsonObject content = candidates.getJsonObject(0).getJsonObject("content");
            if (content == null || content.getJsonArray("parts") == null) {
                throw new LlmException("Gemini returned an empty candidate: " + abbreviate(json));
            }
            StringBuilder sb = new StringBuilder();
            for (JsonObject part : content.getJsonArray("parts").getValuesAs(JsonObject.class)) {
                if (part.containsKey("text") && !part.getBoolean("thought", false)) {
                    sb.append(part.getString("text"));
                }
            }
            return sb.toString();
        } catch (LlmException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LlmException("Unexpected Gemini response: " + abbreviate(json), e);
        }
    }

    static boolean isTransient(int status) {
        return status == 500 || status == 503;
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public String modelName() {
        return model;
    }
}
