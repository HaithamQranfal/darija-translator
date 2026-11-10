package ma.aui.translator.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/** Allows browser-based clients (web pages, Expo web) to call the API. Chrome extensions use host_permissions. */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        res.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
        res.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.getHeaders().putSingle("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-Requested-With");
        res.getHeaders().putSingle("Access-Control-Max-Age", "3600");
    }
}
