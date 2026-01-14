package org.jabref.http.server.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.command.CommandSelectionTab;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.ws.rs.core.Response;

@JsonTypeName("selectentries")
public class SelectEntriesCommand extends Command {

    @JsonProperty
    private String libraryId = "";

    @JsonProperty
    private List<String> citationKeys = new ArrayList<>();

    @JsonProperty
    private List<String> entryIds = new ArrayList<>();

    public SelectEntriesCommand() {
    }

    @Override
    public Response execute() {
        Optional<CommandSelectionTab> activeTab = getSrvStateManager().getActiveSelectionTabProperty().getValue();
        if (activeTab.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("This command cannot be executed because no library is opened.")
                           .build();
        }

        CommandSelectionTab commandSelectionTab = activeTab.get();

        if (!getLibraryIdFromContext(commandSelectionTab.getBibDatabaseContext()).equals(libraryId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("This command cannot be executed because the libraryId does not match the active selection tab.")
                           .build();
        }

        List<BibEntry> entries = commandSelectionTab.getBibDatabaseContext().getEntries().stream()
                                                    .filter(entry -> citationKeys.contains(entry.getCitationKey().orElse(null)) || entryIds.contains(entry.getId()))
                                                    .collect(Collectors.toList());

        commandSelectionTab.clearAndSelect(entries);

        return Response.ok().build();
    }

    private String getLibraryIdFromContext(BibDatabaseContext bibDatabaseContext) {
        return bibDatabaseContext.getDatabasePath()
                                 .map(path -> path.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(path))
                                 .orElse("");
    }
}
