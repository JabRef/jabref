package org.jabref.http.server.command;

import java.util.List;

import org.jabref.logic.UiCommand;
import org.jabref.logic.UiMessageHandler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.ServiceLocator;

/// Example calls are documented at `jabsrv/src/test/commands.http`
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "command")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SelectEntriesCommand.class, name = "selectentries"),
        @JsonSubTypes.Type(value = AppendBibTeXCommand.class, name = "appendbibtex"),
        @JsonSubTypes.Type(value = OpenLibrariesCommand.class, name = "open"),
        @JsonSubTypes.Type(value = FocusCommand.class, name = "focus")
})
public abstract class Command {

    @JsonIgnore
    private ServiceLocator serviceLocator;

    public void setServiceLocator(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    public abstract Response execute();

    protected Response execute(UiCommand uiCommand) {
        UiMessageHandler uiMessageHandler = serviceLocator.getService(UiMessageHandler.class);
        if (uiMessageHandler == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("This command is not supported in CLI mode.")
                           .build();
        }
        uiMessageHandler.handleUiCommands(List.of(uiCommand));

        // RESTish response "accepted", because the UI processes the command asynchronously - and we do not know the result of the processing
        return Response.accepted().build();
    }
}
