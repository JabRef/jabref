package org.jabref.http.server.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.ServiceLocator;

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
        } catch (JsonProcessingException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
