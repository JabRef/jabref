package org.jabref.http.server.command;

import java.util.List;

import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.UiCommand;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.ServiceLocator;

@JsonTypeName("focus")
public class FocusCommand implements Command {

    @JsonIgnore
    private ServiceLocator serviceLocator;

    public FocusCommand() {
    }

    @Override
    public Response execute() {
        UiMessageHandler handler = getServiceLocator().getService(UiMessageHandler.class);
        if (handler == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("UiMessageHandler not available.")
                          .build();
        }

        UiCommand command = new UiCommand.Focus();
        handler.handleUiCommands(List.of(command));

        return Response.ok().build();
    }

    @Override
    public void setServiceLocator(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public ServiceLocator getServiceLocator() {
        return this.serviceLocator;
    }
}
