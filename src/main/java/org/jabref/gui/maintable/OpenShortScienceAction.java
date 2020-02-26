package org.jabref.gui.maintable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.BooleanExpression;

import org.apache.http.client.utils.URIBuilder;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import static org.jabref.gui.actions.ActionHelper.isFieldSetForSelectedEntry;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;


public class OpenShortScienceAction extends SimpleCommand {
    private final DialogService dialogService;
    private final StateManager stateManager;
    private static final String BASIC_SEARCH_URL = "https://www.shortscience.org/internalsearch";

    public OpenShortScienceAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        BooleanExpression fieldIsSet = isFieldSetForSelectedEntry(StandardField.TITLE, stateManager);
        this.executable.bind(needsEntriesSelected(1, stateManager).and(fieldIsSet));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            final List<BibEntry> bibEntries = stateManager.getSelectedEntries();

            if (bibEntries.size() != 1) {
                dialogService.notify(Localization.lang("This operation requires exactly one item to be selected."));
                return;
            }
            getShortScienceSearchURL(bibEntries.get(0)).ifPresent(url -> {
                try {
                    JabRefDesktop.openExternalViewer(databaseContext, url, StandardField.URL);
                } catch (IOException ex) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to open ShortScience."), ex);
                }
            });
        });
    }

    /**
     * Get a URL to the search results of ShortScience for the BibEntry's title
     * @param entry The entry to search for. Expects TITLE to be set for successful return.
     * @return The URL if it was successfully created
     */
    public static Optional<String> getShortScienceSearchURL(BibEntry entry) {
        Optional<String> title = entry.getField(StandardField.TITLE);
        if (!title.isPresent()) {
            return Optional.empty();
        }
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(BASIC_SEARCH_URL);
        } catch (URISyntaxException e) {
            // This should never be able to happen as it would require the field to be misconfigured.
            throw new AssertionError("ShortScience URL is invalid.");
        }
        // Direct the user to the search results for the title.
        uriBuilder.addParameter("q", title.get());
        return Optional.of(uriBuilder.toString());
    }
}
