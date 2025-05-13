package org.jabref.http.server;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class RootResource {
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String get() {
        return """
<html>
<body>
<p>
  JabRef http API runs. Please navigate to <a href="libraries">libraries</a>.
</p>
</body>
""";
    }
}
