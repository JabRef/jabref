package org.jabref.http.server.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.JabRefSrvStateManager;
import org.jabref.logic.command.CommandSelectionTab;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.ServiceLocator;

@JsonTypeName("selectentries")
public class SelectEntriesCommand implements Command {

    @JsonIgnore
    private ServiceLocator serviceLocator;

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
        if (getSrvStateManager() instanceof JabRefSrvStateManager) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("This command is not supported in CLI mode.")
                           .build();
        }

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

    @Override
    public void setServiceLocator(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public ServiceLocator getServiceLocator() {
        return this.serviceLocator;
    }

    public String getLibraryId() {
        return libraryId;
    }

    public List<String> getCitationKeys() {
        return citationKeys;
    }

    public List<String> getEntryIds() {
        return entryIds;
    }

    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }

    public void setCitationKeys(List<String> citationKeys) {
        this.citationKeys = citationKeys;
    }

    public void setEntryIds(List<String> entryIds) {
        this.entryIds = entryIds;
    }
}
