package org.jabref.http.server.resources.callback;

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

/// This resource is triggered by [org.jabref.gui.citedrive.CiteDriveOAuthService]
@Path("/callback")
public class CallbackResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackResource.class);

    @Inject
    OAuthSessionRegistry sessionRegistry;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response citeDriveCallback(@QueryParam("code") String code,
                                      @QueryParam("state") String state,
                                      @QueryParam("error") String error,
                                      @QueryParam("error_description") String errorDescription) {
        if (error != null && !error.isBlank()) {
            LOGGER.warn("CiteDrive CallbackResource error: {} ({}) (state={})", error, errorDescription, state);
            sessionRegistry.fail(state, new IllegalStateException("CallbackResource error: " + error));
            return Response.serverError().build();
        }

        if (code == null || state == null) {
            LOGGER.warn("Missing code or state in CiteDrive callback: code={}, state={}", code, state);
            sessionRegistry.fail(state, new IllegalStateException("Missing code or state"));
            return Response.serverError().build();
        }

        LOGGER.debug("Received CiteDrive callback: state={}, code={}", state, code);
        sessionRegistry.complete(state, code);

        return Response.noContent().build();
    }
}
