package ma.aui.translator.security;

import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Logger;

/**
 * Installs {@link BasicAuthServerAuthModule} for this web application at start-up using the
 * Jakarta Authentication 3.0 API {@link AuthConfigFactory#registerServerAuthModule(
 * jakarta.security.auth.message.module.ServerAuthModule, Object)}.
 */
@WebListener
public class AuthModuleRegistrar implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AuthModuleRegistrar.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();
        CredentialStore store = CredentialStore.fromConfig();
        String id = AuthConfigFactory.getFactory()
                .registerServerAuthModule(new BasicAuthServerAuthModule(store), ctx);
        LOG.info(() -> "Jakarta Authentication: registered BasicAuthServerAuthModule (" + id + ") for "
                + ctx.getContextPath() + " with " + store.size() + " user(s)");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        AuthConfigFactory.getFactory().removeServerAuthModule(event.getServletContext());
    }
}
