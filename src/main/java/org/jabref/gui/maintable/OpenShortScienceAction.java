package org.jabref.gui.maintable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;

import org.apache.http.client.utils.URIBuilder;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;


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
            Optional<String> title = bibEntries.get(0).getField(StandardField.TITLE);
            if (!title.isPresent()) {
                // TODO: Consider outputting feedback to user?
                return;
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
            try {
                JabRefDesktop.openExternalViewer(databaseContext, uriBuilder.toString(), StandardField.URL);
            } catch (IOException ex) {
                dialogService.showErrorDialogAndWait(Localization.lang("Unable to open ShortScience."), ex);
            }
        });
    }

    /*
     * TODO: These three methods are taken from ActionHelper in PR #5958, but were part of a combined
     *       commit there, making them hard to cherry-pick. They should be removed when that PR is
     *       merged.
     */
    public static BooleanExpression needsEntriesSelected(int numberOfEntries, StateManager stateManager) {
        return Bindings.createBooleanBinding(
                () -> stateManager.getSelectedEntries().size() == numberOfEntries,
                stateManager.getSelectedEntries());
    }

    public static BooleanExpression isFieldSetForSelectedEntry(Field field, StateManager stateManager) {
        return isAnyFieldSetForSelectedEntry(Collections.singletonList(field), stateManager);
    }

    public static BooleanExpression isAnyFieldSetForSelectedEntry(List<Field> fields, StateManager stateManager) {
        BibEntry entry = stateManager.getSelectedEntries().get(0);
        return Bindings.createBooleanBinding(
                () -> entry.getFields().stream().anyMatch(fields::contains),
                entry.getFieldsObservable(),
                stateManager.getSelectedEntries());
    }
}
