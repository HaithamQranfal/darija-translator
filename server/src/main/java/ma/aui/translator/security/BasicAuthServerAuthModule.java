package ma.aui.translator.security;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.callback.CallerPrincipalCallback;
import jakarta.security.auth.message.callback.GroupPrincipalCallback;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * HTTP Basic authentication implemented as a Jakarta Authentication (JASPIC) {@link ServerAuthModule}.
 * <p>
 * The container calls {@link #validateRequest} before every request reaches the Servlet/REST layer:
 * <ul>
 *   <li>valid {@code Authorization: Basic base64(user:password)} header -> the caller principal and its
 *       groups are handed to the container through {@link CallerPrincipalCallback}/{@link GroupPrincipalCallback};</li>
 *   <li>missing/invalid credentials on a protected URL (security-constraint in web.xml) -> 401 challenge;</li>
 *   <li>public URL (e.g. /api/health) without credentials -> request continues anonymously.</li>
 * </ul>
 * Registered programmatically by {@link AuthModuleRegistrar}.
 */
public class BasicAuthServerAuthModule implements ServerAuthModule {

    public static final String REALM = "DarijaTranslator";
    static final String IS_MANDATORY = "jakarta.security.auth.message.MessagePolicy.isMandatory";
    private static final String AUTH_TYPE = "jakarta.servlet.http.authType";
    private static final Logger LOG = Logger.getLogger(BasicAuthServerAuthModule.class.getName());

    private final CredentialStore store;
    private CallbackHandler handler;

    public BasicAuthServerAuthModule() {
        this(CredentialStore.fromConfig());
    }

    public BasicAuthServerAuthModule(CredentialStore store) {
        this.store = store;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
                           CallbackHandler handler, Map options) {
        this.handler = handler;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    @SuppressWarnings("unchecked")
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject client, Subject service) throws AuthException {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        boolean mandatory = Boolean.parseBoolean(String.valueOf(messageInfo.getMap().get(IS_MANDATORY)));

        // CORS pre-flight requests never carry credentials.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return anonymous(client);
        }

        Optional<String[]> credentials = parseBasic(request.getHeader("Authorization"));
        if (credentials.isPresent()) {
            String user = credentials.get()[0];
            Optional<List<String>> groups = store.validate(user, credentials.get()[1]);
            if (groups.isPresent()) {
                notify(new CallerPrincipalCallback(client, user),
                        new GroupPrincipalCallback(client, groups.get().toArray(String[]::new)));
                messageInfo.getMap().put(AUTH_TYPE, "BASIC");
                return AuthStatus.SUCCESS;
            }
            LOG.fine(() -> "Rejected credentials for user '" + user + "'");
            return challenge(request, response, "Invalid username or password");
        }
        if (mandatory) {
            return challenge(request, response, "Authentication required (HTTP Basic)");
        }
        return anonymous(client);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject service) {
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) {
        if (subject != null) {
            subject.getPrincipals().clear();
        }
    }

    /** Decodes "Basic base64(user:password)" -> [user, password]. */
    static Optional<String[]> parseBasic(String header) {
        if (header == null || header.length() < 6 || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return Optional.empty();
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
            int i = decoded.indexOf(':');
            if (i <= 0) return Optional.empty();
            return Optional.of(new String[]{decoded.substring(0, i), decoded.substring(i + 1)});
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private AuthStatus anonymous(Subject client) throws AuthException {
        notify(new CallerPrincipalCallback(client, (Principal) null));
        return AuthStatus.SUCCESS;
    }

    private AuthStatus challenge(HttpServletRequest request, HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // Programmatic clients (extension, mobile app) send X-Requested-With to avoid the browser's login pop-up.
            String scheme = request.getHeader("X-Requested-With") != null ? "xBasic" : "Basic";
            response.setHeader("WWW-Authenticate", scheme + " realm=\"" + REALM + "\", charset=\"UTF-8\"");
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\""
                    + message.replace("\"", "'") + "\"}");
        } catch (IOException e) {
            LOG.warning("Could not write 401 response: " + e.getMessage());
        }
        return AuthStatus.SEND_FAILURE;
    }

    private void notify(Callback... callbacks) throws AuthException {
        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            AuthException ae = new AuthException("Callback handling failed: " + e.getMessage());
            ae.initCause(e);
            throw ae;
        }
    }
}
