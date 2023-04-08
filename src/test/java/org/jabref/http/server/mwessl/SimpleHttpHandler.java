package org.jabref.http.server.mwessl;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * Simple {@link HttpHandler} implementation.
 */
public class SimpleHttpHandler extends HttpHandler {

    public void service(final Request request, final Response response) throws Exception {
        response.setContentType("text/plain");
        response.getWriter().write("Hello world!");
    }
}
