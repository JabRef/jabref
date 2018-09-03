package org.jabref.gui.actions;

import org.jabref.gui.DialogService;
import org.jabref.gui.EntryTypeView;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.plaintextimport.TextInputDialog;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.UpdateFieldPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryFromPlainTextAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryFromPlainTextAction.class);

    private final UpdateFieldPreferences prefs;
    private final JabRefFrame jabRefFrame;
    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public NewEntryFromPlainTextAction(JabRefFrame jabRefFrame, UpdateFieldPreferences prefs, DialogService dialogService, JabRefPreferences preferences) {
        this.jabRefFrame = jabRefFrame;
        this.prefs = prefs;
        this.dialogService = dialogService;
        this.preferences = preferences;

    }

    @Override
    public void execute() {
        if (jabRefFrame.getBasePanelCount() <= 0) {
            LOGGER.error("Action 'New entry' must be disabled when no database is open.");
            return;
        }

        EntryTypeView typeChoiceDialog = new EntryTypeView(jabRefFrame.getCurrentBasePanel(), dialogService, preferences);
        EntryType selectedType = typeChoiceDialog.showAndWait().orElse(null);
        if (selectedType == null) {
            return;
        }
        BibEntry bibEntry = new BibEntry(selectedType);

        TextInputDialog tidialog = new TextInputDialog(jabRefFrame, bibEntry);
        tidialog.setVisible(true);
        if (tidialog.okPressed()) {
            UpdateField.setAutomaticFields(bibEntry, false, false, prefs);
            jabRefFrame.getCurrentBasePanel().insertEntry(bibEntry);
        }
    }
}
