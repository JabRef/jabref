package org.jabref.http.server;

import java.util.List;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.server.ConnectorTokenManager;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class SecurityFilter implements ContainerRequestFilter {

    private static final String CUSTOM_HEADER = "X-JabRef-Connector";
    private static final String BEARER_PREFIX = "Bearer ";

    enum OriginMatch {
        NONE,
        EXACT,
        PREFIX
    }

    @Inject
    CliPreferences preferences;

    @Inject
    ConnectorTokenManager tokenManager;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        if ("/".equals(path) || path.isEmpty()) {
            return;
        }

        String origin = requestContext.getHeaderString("Origin");
        if (origin == null) {
            return;
        }

        OriginMatch matchType = classifyOrigin(origin);
        if (matchType == OriginMatch.NONE) {
            requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                            .entity("Origin not allowed")
                            .build());
            return;
        }

        if (matchType == OriginMatch.EXACT) {
            return;
        }

        String customHeader = requestContext.getHeaderString(CUSTOM_HEADER);
        if (customHeader == null) {
            requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                            .entity("Missing required header: " + CUSTOM_HEADER)
                            .build());
            return;
        }

        boolean isPairingEndpoint = path.startsWith("auth/pair");
        if (isPairingEndpoint) {
            return;
        }

        if (tokenManager == null) {
            requestContext.abortWith(
                    Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .entity("Token authentication not available")
                            .build());
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("Missing or invalid Authorization header")
                            .build());
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!tokenManager.validateToken(token)) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("Invalid token")
                            .build());
        }
    }

    OriginMatch classifyOrigin(String origin) {
        if (preferences == null) {
            return OriginMatch.NONE;
        }

        RemotePreferences remotePreferences = preferences.getRemotePreferences();
        List<String> allowedOrigins = remotePreferences.getAllowedOrigins();

        for (String allowed : allowedOrigins) {
            if (allowed.endsWith("://")) {
                if (origin.startsWith(allowed)) {
                    return OriginMatch.PREFIX;
                }
            } else if (origin.equals(allowed)) {
                return OriginMatch.EXACT;
            }
        }
        return OriginMatch.NONE;
    }
}
