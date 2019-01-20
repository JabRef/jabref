package org.jabref.gui.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.EntryTypeView;
import org.jabref.gui.JabRefFrame;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryAction.class);

    private final JabRefFrame jabRefFrame;
    /**
     * The type of the entry to create.
     */
    private final Optional<EntryType> type;
    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public NewEntryAction(JabRefFrame jabRefFrame, DialogService dialogService, JabRefPreferences preferences) {
        this.jabRefFrame = jabRefFrame;
        this.type = Optional.empty();
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    public NewEntryAction(JabRefFrame jabRefFrame, EntryType type, DialogService dialogService, JabRefPreferences preferences) {
        this.jabRefFrame = jabRefFrame;
        this.type = Optional.of(type);
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void execute() {
        if (jabRefFrame.getBasePanelCount() <= 0) {
            LOGGER.error("Action 'New entry' must be disabled when no database is open.");
            return;
        }

        if (type.isPresent()) {
            jabRefFrame.getCurrentBasePanel().insertEntry(new BibEntry(type.get()));
        } else {
            EntryTypeView typeChoiceDialog = new EntryTypeView(jabRefFrame.getCurrentBasePanel(), dialogService, preferences);
            EntryType selectedType = typeChoiceDialog.showAndWait().orElse(null);
            if (selectedType == null) {
                return;
            }

            trackNewEntry(selectedType);
            jabRefFrame.getCurrentBasePanel().insertEntry(new BibEntry(selectedType));
        }
    }

    private void trackNewEntry(EntryType type) {
        Map<String, String> properties = new HashMap<>();
        properties.put("EntryType", type.getName());

        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, new HashMap<>()));
    }
}
