package org.jabref.http.server;

import java.util.List;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.RemotePreferences;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {

    @Inject
    CliPreferences preferences;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString("Origin");

        if (origin != null && isOriginAllowed(origin)) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        }

        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, X-JabRef-Connector, Authorization");
        responseContext.getHeaders().add("Access-Control-Max-Age", "600");
        responseContext.getHeaders().add("Vary", "Origin");
    }

    private boolean isOriginAllowed(String origin) {
        if (preferences == null) {
            return false;
        }

        RemotePreferences remotePreferences = preferences.getRemotePreferences();
        List<String> allowedOrigins = remotePreferences.getAllowedOrigins();

        for (String allowed : allowedOrigins) {
            if (allowed.endsWith("://")) {
                if (origin.startsWith(allowed)) {
                    return true;
                }
            } else if (origin.equals(allowed)) {
                return true;
            }
        }
        return false;
    }
}
