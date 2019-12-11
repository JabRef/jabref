package org.jabref.gui.importer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.*;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class NewEntryAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryAction.class);

    private final JabRefFrame jabRefFrame;
    /**
     * The type of the entry to create.
     */
    private Optional<EntryType> type;
    private boolean fromID;

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public NewEntryAction(JabRefFrame jabRefFrame, DialogService dialogService, JabRefPreferences preferences, StateManager stateManager) {
        this.jabRefFrame = jabRefFrame;
        this.dialogService = dialogService;
        this.preferences = preferences;

        this.type = Optional.empty();
        this.fromID = false;

        this.executable.bind(needsDatabase(stateManager));
    }

    public NewEntryAction(JabRefFrame jabRefFrame, EntryType type, DialogService dialogService, JabRefPreferences preferences, StateManager stateManager) {
        this(jabRefFrame, dialogService, preferences, stateManager);
        this.type = Optional.of(type);
        this.fromID = false;
    }

    public NewEntryAction(JabRefFrame jabRefFrame, boolean fromID, DialogService dialogService, JabRefPreferences preferences, StateManager stateManager) {
        this.jabRefFrame = jabRefFrame;
        this.dialogService = dialogService;
        this.preferences = preferences;

        this.type = Optional.empty();
        this.fromID = fromID;
    }

    @Override
    public void execute() {
        if (jabRefFrame.getBasePanelCount() <= 0) {
            LOGGER.error("Action 'New entry' must be disabled when no database is open.");
            return;
        }

        if (fromID){
            EntryFromIDView idDialog = new EntryFromIDView(jabRefFrame.getCurrentBasePanel(), dialogService, preferences);
            EntryType selectedType = idDialog.showAndWait().orElse(null);
            if (selectedType == null) {
                return;
            }

        } else {
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
    }

    private void trackNewEntry(EntryType type) {
        Map<String, String> properties = new HashMap<>();
        properties.put("EntryType", type.getName());

        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, new HashMap<>()));
    }
}
