package org.jabref.http.server.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.UiCommand;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.ws.rs.core.Response;

@JsonTypeName("open")
public class OpenLibrariesCommand extends Command {

    @JsonProperty(required = true)
    private List<String> paths = new ArrayList<>();

    @Override
    public Response execute() {
        List<Path> pathList = paths.stream()
                                   .map(Path::of)
                                   .toList();
        return execute(new UiCommand.OpenLibraries(pathList));
    }
}
