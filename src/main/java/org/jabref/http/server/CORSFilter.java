package org.jabref.http.server;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String requestOrigin = requestContext.getHeaderString("Origin");
        if (requestOrigin == null) {
            // IntelliJ's rest client is calling
            responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        } else if (requestOrigin.contains("://localhost")) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", requestOrigin);
        }
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept");
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "false");
    }
}
