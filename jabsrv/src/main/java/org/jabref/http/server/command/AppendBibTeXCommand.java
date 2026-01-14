package org.jabref.http.server.command;

import org.jabref.logic.UiCommand;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.ws.rs.core.Response;

@JsonTypeName("appendbibtex")
public class AppendBibTeXCommand extends Command {
    @JsonProperty
    private String bibtex;

    @Override
    public Response execute() {
        return execute(new UiCommand.AppendBibTeXToCurrentLibrary(bibtex));
    }
}
