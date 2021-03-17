package org.jabref.gui.importer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.EntryTypeView;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.preferences.PreferencesService;

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
    private final DialogService dialogService;
    private final PreferencesService preferences;

    public NewEntryAction(JabRefFrame jabRefFrame, DialogService dialogService, PreferencesService preferences, StateManager stateManager) {
        this.jabRefFrame = jabRefFrame;
        this.dialogService = dialogService;
        this.preferences = preferences;

        this.type = Optional.empty();

        this.executable.bind(needsDatabase(stateManager));
    }

    public NewEntryAction(JabRefFrame jabRefFrame, EntryType type, DialogService dialogService, PreferencesService preferences, StateManager stateManager) {
        this(jabRefFrame, dialogService, preferences, stateManager);
        this.type = Optional.of(type);
    }

    @Override
    public void execute() {
        if (jabRefFrame.getBasePanelCount() <= 0) {
            LOGGER.error("Action 'New entry' must be disabled when no database is open.");
            return;
        }

        if (type.isPresent()) {
            jabRefFrame.getCurrentLibraryTab().insertEntry(new BibEntry(type.get()));
        } else {
            EntryTypeView typeChoiceDialog = new EntryTypeView(jabRefFrame.getCurrentLibraryTab(), dialogService, preferences);
            EntryType selectedType = dialogService.showCustomDialogAndWait(typeChoiceDialog).orElse(null);
            if (selectedType == null) {
                return;
            }

            trackNewEntry(selectedType);
            jabRefFrame.getCurrentLibraryTab().insertEntry(new BibEntry(selectedType));
        }
    }

    private void trackNewEntry(EntryType type) {
        Map<String, String> properties = new HashMap<>();
        properties.put("EntryType", type.getName());

        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, new HashMap<>()));
    }
}
