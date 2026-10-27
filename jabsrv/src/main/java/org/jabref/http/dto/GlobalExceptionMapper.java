package org.jabref.http.dto;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof UncheckedFetcherException ufe) {
            // FetcherException#getLocalizedMessage embeds the upstream URL,
            // HTTP status line, and full response body — none of which are
            // safe to send back to the HTTP client. Log the full chain for
            // operators, return a generic body to the caller.
            LOGGER.warn("Plain-citation fetcher failed", ufe.getCause());
            return Response.serverError()
                           .entity("Plain-citation fetcher failed.")
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        }
        if (exception instanceof WebApplicationException webex) {
            Response response = webex.getResponse();
            // Preserve responses that already carry their own body and content type — e.g. the
            // HTML-escaped 404 from EntryResource, which deliberately attaches a text/html entity.
            // Overwriting it would drop the escaping work and change the content type.
            if (response.hasEntity()) {
                return response;
            }
            // Surface the exception message (e.g. the text passed to new BadRequestException("…"))
            // as the body; Jersey otherwise returns a bodyless status and clients can't tell why
            // the request failed.
            return Response.fromResponse(response)
                           .entity(webex.getMessage())
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        }
        LOGGER.error("Unhandled exception on server", exception);
        return Response.serverError().entity("Internal Server Error").build();
    }
}
