package ma.aui.translator.llm;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lightweight local LLM through Ollama (https://ollama.com), e.g. {@code ollama pull qwen2.5:3b}.
 * No API key and no internet needed once the model is pulled.
 */
public class OllamaClient implements LlmClient {

    private final HttpClient http;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;

    public OllamaClient(String baseUrl, String model, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public LlmResult complete(String systemPrompt, String userPrompt) {
        JsonObject body = Json.createObjectBuilder()
                .add("model", model)
                .add("stream", false)
                .add("format", "json")
                .add("options", Json.createObjectBuilder().add("temperature", 0.2))
                .add("messages", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("role", "system").add("content", systemPrompt))
                        .add(Json.createObjectBuilder().add("role", "user").add("content", userPrompt)))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new LlmException("Ollama returned HTTP " + response.statusCode() + ": " + response.body(),
                        response.statusCode(), null);
            }
            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject message = reader.readObject().getJsonObject("message");
                return LlmResponseParser.parse(message == null ? null : message.getString("content", null));
            }
        } catch (IOException e) {
            throw new LlmException("Cannot reach Ollama at " + baseUrl + " (is `ollama serve` running?)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while calling Ollama", e);
        }
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    @Override
    public String modelName() {
        return model;
    }
}
