package ma.aui.translator.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ma.aui.translator.service.TranslationService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Public (unauthenticated) health check used by clients to test connectivity. */
@Path("/health")
public class HealthResource {

    @Inject
    TranslationService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("service", "Darija Translator");
        m.put("provider", service.providerName());
        m.put("model", service.modelName());
        return m;
    }
}
