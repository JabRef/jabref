package org.jabref.http.server.resources.callback;

import org.jabref.logic.JabRefException;
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
            return Response.serverError().entity(failurePage()).build();
        }

        if (error != null && !error.isBlank()) {
            LOGGER.warn("CiteDrive callback error: {} ({})", error, errorDescription);
            sessionRegistry.fail(state, new JabRefException("CiteDrive authorization error: " + error, Localization.lang("Login could not be completed")));
            return Response.serverError().entity(failurePage()).build();
        }

        if (code == null || code.isBlank()) {
            LOGGER.warn("Missing code in CiteDrive callback");
            sessionRegistry.fail(state, new JabRefException("Missing code in CiteDrive callback", Localization.lang("Login could not be completed")));
            return Response.serverError().entity(failurePage()).build();
        }

        if (!sessionRegistry.complete(state, code)) {
            // The login this callback belongs to timed out, or the callback arrived twice
            return Response.serverError().entity(failurePage()).build();
        }
        return Response.ok(page(Localization.lang("Logged in to CiteDrive"), Localization.lang("Continue in JabRef. You can close this window."))).build();
    }

    private static String failurePage() {
        return page(Localization.lang("Login could not be completed"), Localization.lang("Please try again in JabRef. You can close this window."));
    }

    /// The page the user is left with in the browser
    private static String page(String heading, String message) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                  <style>
                    body { margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
                           background: #f5f6f7; color: #27313d;
                           font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
                    main { max-width: 26rem; padding: 2.5rem; text-align: center; background: #fff;
                           border-radius: 0.75rem; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12); }
                    h1 { margin: 0 0 0.75rem; font-size: 1.4rem; font-weight: 600; }
                    p { margin: 0; color: #5b6673; }
                    @media (prefers-color-scheme: dark) {
                      body { background: #1e242b; color: #e7eaee; }
                      main { background: #272e36; box-shadow: none; }
                      p { color: #aab3bd; }
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>%s</h1>
                    <p>%s</p>
                  </main>
                </body>
                </html>
                """.formatted(heading, heading, message);
    }
}
