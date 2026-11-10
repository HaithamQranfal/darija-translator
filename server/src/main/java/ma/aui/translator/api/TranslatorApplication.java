package ma.aui.translator.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Jakarta REST application: every resource is served under {@code /<context-root>/api}. */
@ApplicationPath("/api")
public class TranslatorApplication extends Application {
}
