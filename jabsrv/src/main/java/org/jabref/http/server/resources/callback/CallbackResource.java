package org.jabref.http.server.resources.callback;

import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.l10n.Localization;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The browser is redirected here after logging in at CiteDrive, see [org.jabref.logic.citedrive.CiteDriveOAuthService]
@NullMarked
@Path("/callback")
public class CallbackResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackResource.class);

    @Inject
    OAuthSessionRegistry sessionRegistry;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response citeDriveCallback(@QueryParam("code") @Nullable String code,
                                      @QueryParam("state") @Nullable String state,
                                      @QueryParam("error") @Nullable String error,
                                      @QueryParam("error_description") @Nullable String errorDescription) {
        if (state == null || state.isBlank()) {
            // Without state, no pending login can be identified
            LOGGER.warn("Missing state in CiteDrive callback (error: {})", error);
            return Response.serverError().entity("<html><body>" + Localization.lang("Missing information. You can close this window.") + "</body></html>").build();
        }

        if (error != null && !error.isBlank()) {
            LOGGER.warn("CiteDrive callback error: {} ({})", error, errorDescription);
            sessionRegistry.fail(state, new IllegalStateException("CiteDrive authorization error: " + error));
            return Response.serverError().entity("<html><body>" + Localization.lang("Authorization failed. You can close this window.") + "</body></html>").build();
        }

        if (code == null || code.isBlank()) {
            LOGGER.warn("Missing code in CiteDrive callback");
            sessionRegistry.fail(state, new IllegalStateException("Missing code"));
            return Response.serverError().entity("<html><body>" + Localization.lang("Missing information. You can close this window.") + "</body></html>").build();
        }

        sessionRegistry.complete(state, code);
        return Response.ok("<html><body>" + Localization.lang("Authorization successful. You can close this window.") + "</body></html>").build();
    }
}
