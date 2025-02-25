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
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

public class OpenFileAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;

    public OpenFileAction(DialogService dialogService, StateManager stateManager, GuiPreferences preferences) {
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

            if (entry.hasField(StandardField.FILE) && !entry.getFiles().isEmpty()) { // Check if files exist
                LinkedFile linkedFile = entry.getFiles().getFirst();
                try {
                    Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByLinkedFile(linkedFile, true, preferences.getExternalApplicationsPreferences());
                    boolean successful = NativeDesktop.openExternalFileAnyFormat(databaseContext, preferences.getExternalApplicationsPreferences(), preferences.getFilePreferences(), linkedFile.getLink(), type);
                    if (!successful) {
                        dialogService.showErrorDialogAndWait(Localization.lang("File not found"), Localization.lang("Could not find file '%0'.", linkedFile.getLink()));
                    }
                } catch (IOException e) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Error opening file '%0'", linkedFile.getLink()), e);
                }
            } else {
                dialogService.notify(Localization.lang("No file is attached to the selected entry."));
            }
        });
    }
}
