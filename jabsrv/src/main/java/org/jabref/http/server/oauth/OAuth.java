package org.jabref.http.server.oauth;

import org.jabref.logic.citedrive.OAuthSessionRegistry;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/oauth")
public class OAuth {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth.class);

    @Inject
    OAuthSessionRegistry sessionRegistry;

    @GET
    @Path("/citedrive")
    @Produces(MediaType.TEXT_HTML)
    public Response citeDriveCallback(@QueryParam("code") String code,
                                      @QueryParam("state") String state,
                                      @QueryParam("error") String error) {
        if (error != null && !error.isBlank()) {
            LOGGER.warn("CiteDrive OAuth error: {} (state={})", error, state);
            sessionRegistry.fail(state, new IllegalStateException("OAuth error: " + error));
            return Response.serverError().entity("<html><body>Authorization failed. You can close this window.</body></html>").build();
        }

        if (code == null || state == null) {
            LOGGER.warn("Missing code or state in CiteDrive callback: code={}, state={}", code, state);
            sessionRegistry.fail(state, new IllegalStateException("Missing code or state"));
            return Response.serverError().entity("<html><body>Missing information. You can close this window.</body></html>").build();
        }

        LOGGER.debug("Received CiteDrive callback: state={}, code={}", state, code);
        sessionRegistry.complete(state, code);

        return Response.ok("<html><body>Authorization successful. You can close this window.</body></html>").build();
    }
}
