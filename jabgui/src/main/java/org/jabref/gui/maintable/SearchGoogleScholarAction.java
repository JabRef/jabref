package org.jabref.gui.maintable;

import java.io.IOException;
import java.util.List;

import javafx.beans.binding.BooleanExpression;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.ExternalLinkCreator;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import static org.jabref.gui.actions.ActionHelper.isFieldSetForSelectedEntry;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class SearchGoogleScholarAction extends SimpleCommand {
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final ExternalLinkCreator externalLinkCreator;

    public SearchGoogleScholarAction(DialogService dialogService, StateManager stateManager, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;

        this.externalLinkCreator = new ExternalLinkCreator(preferences.getImporterPreferences());

        BooleanExpression fieldIsSet = isFieldSetForSelectedEntry(StandardField.TITLE, stateManager);
        this.executable.bind(needsEntriesSelected(1, stateManager).and(fieldIsSet));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            final List<BibEntry> bibEntries = stateManager.getSelectedEntries();
            externalLinkCreator.getGoogleScholarSearchURL(bibEntries.getFirst()).ifPresent(url -> {
                try {
                    NativeDesktop.openExternalViewer(databaseContext, preferences, url, StandardField.URL, dialogService, bibEntries.getFirst());
                } catch (IOException ex) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to open Google Scholar."), ex);
                }
            });
        });
    }
}
