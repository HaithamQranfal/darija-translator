package ma.aui.translator.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import ma.aui.translator.api.dto.TranslationRequest;
import ma.aui.translator.api.dto.TranslationResponse;
import ma.aui.translator.service.TranslationService;

import java.util.Map;

/**
 * The TranslatorResource RESTful endpoint. Protected by HTTP Basic authentication
 * (see {@code security.BasicAuthServerAuthModule} and {@code WEB-INF/web.xml}).
 *
 * <pre>
 * POST /api/translator/translate   {"text":"How are you?"}         -> JSON TranslationResponse
 * POST /api/translator/translate   text/plain body                 -> text/plain Darija
 * GET  /api/translator/translate?text=How%20are%20you%3F           -> JSON TranslationResponse
 * GET  /api/translator/whoami                                      -> authenticated caller
 * </pre>
 */
@Path("/translator")
@RequestScoped
public class TranslatorResource {

    @Inject
    TranslationService service;

    @Context
    SecurityContext security;

    /** Business method: accepts a text (JSON) and returns its Darija translation. */
    @POST
    @Path("/translate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TranslationResponse translate(TranslationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required: {\"text\": \"...\"}");
        }
        return service.translate(request.getText(), request.getSourceLanguage());
    }

    /** Same business method for quick tests with cURL: plain text in, plain Darija out. */
    @POST
    @Path("/translate")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN + ";charset=UTF-8")
    public String translatePlain(String text) {
        return service.translate(text, "auto").getTranslation();
    }

    /** GET variant so the endpoint can also be tried from a browser address bar. */
    @GET
    @Path("/translate")
    @Produces(MediaType.APPLICATION_JSON)
    public TranslationResponse translateQuery(@QueryParam("text") String text,
                                              @QueryParam("source") @DefaultValue("auto") String source) {
        return service.translate(text, source);
    }

    /** Returns who the Jakarta Authentication module authenticated (handy to demo the security layer). */
    @GET
    @Path("/whoami")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> whoami() {
        String user = security.getUserPrincipal() == null ? null : security.getUserPrincipal().getName();
        return Map.of(
                "user", user == null ? "anonymous" : user,
                "authenticationScheme", String.valueOf(security.getAuthenticationScheme()),
                "inRoleTranslator", security.isUserInRole("translator"));
    }
}
