package ma.aui.translator.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ma.aui.translator.api.dto.ErrorResponse;
import ma.aui.translator.llm.LlmException;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Maps exceptions to consistent JSON error responses. */
public final class ExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ExceptionMappers.class.getName());

    private ExceptionMappers() {
    }

    static Response json(Response.Status status, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), message))
                .build();
    }

    /** Invalid input -> 400. */
    @Provider
    public static class BadRequestMapper implements ExceptionMapper<IllegalArgumentException> {
        @Override
        public Response toResponse(IllegalArgumentException e) {
            return json(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    /** LLM failure -> 502 (or 429 when the free-tier quota is exhausted). */
    @Provider
    public static class LlmMapper implements ExceptionMapper<LlmException> {
        @Override
        public Response toResponse(LlmException e) {
            LOG.log(Level.WARNING, "LLM call failed", e);
            Response.Status status = e.getUpstreamStatus() == 429
                    ? Response.Status.TOO_MANY_REQUESTS : Response.Status.BAD_GATEWAY;
            return json(status, e.getMessage());
        }
    }

    /** Keep JAX-RS errors (404, 405, 415...) but give them a JSON body. */
    @Provider
    public static class WebAppMapper implements ExceptionMapper<WebApplicationException> {
        @Override
        public Response toResponse(WebApplicationException e) {
            Response.Status status = Response.Status.fromStatusCode(e.getResponse().getStatus());
            if (status == null) return e.getResponse();
            return json(status, e.getMessage());
        }
    }
}
