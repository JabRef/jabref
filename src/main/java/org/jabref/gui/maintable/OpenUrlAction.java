package org.jabref.gui.maintable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.BooleanExpression;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class OpenUrlAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;

    public OpenUrlAction(DialogService dialogService, StateManager stateManager, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;

        BooleanExpression fieldIsSet = ActionHelper.isAnyFieldSetForSelectedEntry(
                List.of(StandardField.URL, StandardField.DOI, StandardField.URI, StandardField.EPRINT),
                stateManager);
        this.executable.bind(ActionHelper.needsEntriesSelected(1, stateManager).and(fieldIsSet));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            final List<BibEntry> entries = stateManager.getSelectedEntries();

            if (entries.size() != 1) {
                dialogService.notify(Localization.lang("This operation requires exactly one item to be selected."));
                return;
            }

            BibEntry entry = entries.getFirst();

            // ToDo: Create dialog or menu to chose which one to open
            // URL - DOI - DOI - EPRINT
            Optional<String> link = entry.getField(StandardField.EPRINT);
            Field field = StandardField.EPRINT;
            if (entry.hasField(StandardField.URI)) {
                link = entry.getField(StandardField.URI);
                field = StandardField.URI;
            }
            if (entry.hasField(StandardField.ISBN)) {
                link = entry.getField(StandardField.ISBN);
                field = StandardField.ISBN;
            }
            if (entry.hasField(StandardField.DOI)) {
                link = entry.getField(StandardField.DOI);
                field = StandardField.DOI;
            }
            if (entry.hasField(StandardField.URL)) {
                link = entry.getField(StandardField.URL);
                field = StandardField.URL;
            }

            if (link.isPresent()) {
                try {
                    if (field.equals(StandardField.DOI) && preferences.getDOIPreferences().isUseCustom()) {
                        NativeDesktop.openCustomDoi(link.get(), preferences, dialogService);
                    } else {
                        NativeDesktop.openExternalViewer(databaseContext, preferences, link.get(), field, dialogService, entry);
                    }
                } catch (IOException e) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), e);
                }
            }
        });
    }
}
