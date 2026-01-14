package org.jabref.http.server.command;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.UiCommand;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.ws.rs.core.Response;

@JsonTypeName("selectentries")
public class SelectEntriesCommand extends Command {

    // TODO: This is probably needed for some CAYW functionality to "scope" the citation keys properly
    // @JsonProperty
    // private String libraryId = "";

    @JsonProperty(required = true)
    private List<String> citationKeys = new ArrayList<>();

    // TODO: This is probably needed for some CAYW functionality to use a unique entry id "scope" the citation keys properly
    // @JsonProperty
    // private List<String> entryIds = new ArrayList<>();

    @Override
    public Response execute() {
        return execute(new UiCommand.SelectEntryKeys(citationKeys));
    }
}
