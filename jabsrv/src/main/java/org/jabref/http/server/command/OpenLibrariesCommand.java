package org.jabref.http.server.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.UiCommand;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.ServiceLocator;

@JsonTypeName("open")
public class OpenLibrariesCommand implements Command {

    @JsonIgnore
    private ServiceLocator serviceLocator;

    @JsonProperty
    private List<String> paths = new ArrayList<>();

    public OpenLibrariesCommand() {
    }

    @Override
    public Response execute() {
        UiMessageHandler handler = getServiceLocator().getService(UiMessageHandler.class);
        if (handler == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("UiMessageHandler not available.")
                          .build();
        }

        List<Path> pathList = paths.stream()
                                   .map(Path::of)
                                   .toList();

        UiCommand command = new UiCommand.OpenLibraries(pathList);
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

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}
