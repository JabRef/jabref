package org.jabref.gui.maintable;

import javafx.beans.binding.BooleanExpression;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

import java.util.List;

import static org.jabref.gui.actions.ActionHelper.isFieldSetForSelectedEntry;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class LookupAuthorAction extends SimpleCommand {

    protected final DialogService dialogService;
    protected final StateManager stateManager;
    protected final PreferencesService preferencesService;
    public final String baseUrl;
    public final String arthurName;

    public LookupAuthorAction( String name, DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.baseUrl = "https://scholar.google.com/citations?view_op=search_authors&";
        this.arthurName = name;
        BooleanExpression fieldIsSet = isFieldSetForSelectedEntry(StandardField.TITLE, stateManager);
        this.executable.bind(needsEntriesSelected(1, stateManager).and(fieldIsSet));
    }
    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            final List<BibEntry> entries = stateManager.getSelectedEntries();
            String query = "";
            if (entries.size() != 1) {
                dialogService.notify(Localization.lang("This operation requires exactly one item to be selected."));
                return;
            }
            query = "mauthors=" + arthurName.replace(" ","+");
            String url = baseUrl + query ;
            System.out.println(url);
            JabRefDesktop.openBrowserShowPopup(url);
        });
    }
}
