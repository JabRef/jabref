package org.jabref.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class RootResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response rootPage() {
        return Response.ok("<html>\n"
                + "<body>\n"
                + "<p>\n"
                + "This is JabRefs's API."
                + "</p>\n"
                + "</body>\n"
                + "\n").build();
    }
}
