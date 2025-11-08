package org.jabref.http.server.command;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.ServiceLocator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Path("commands")
public class CommandResource {

    @Inject
    private ServiceLocator serviceLocator;

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response dispatchCommand(String jsonCommand) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Command command = objectMapper.readValue(jsonCommand, Command.class);
            command.setServiceLocator(serviceLocator);

            return command.execute();
        } catch (JacksonException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
