package org.jabref.gui.importer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.EntryTypeView;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.Telemetry;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryAction.class);

    private final Supplier<LibraryTab> tabSupplier;

    /**
     * The type of the entry to create.
     */
    private Optional<EntryType> type;

    private final DialogService dialogService;

    private final PreferencesService preferences;

    public NewEntryAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, PreferencesService preferences, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.preferences = preferences;

        this.type = Optional.empty();

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    public NewEntryAction(Supplier<LibraryTab> tabSupplier, EntryType type, DialogService dialogService, PreferencesService preferences, StateManager stateManager) {
        this(tabSupplier, dialogService, preferences, stateManager);
        this.type = Optional.of(type);
    }

    @Override
    public void execute() {
        if (tabSupplier.get() == null) {
            LOGGER.error("Action 'New entry' must be disabled when no database is open.");
            return;
        }

        if (type.isPresent()) {
            tabSupplier.get().insertEntry(new BibEntry(type.get()));
        } else {
            EntryTypeView typeChoiceDialog = new EntryTypeView(tabSupplier.get(), dialogService, preferences);
            EntryType selectedType = dialogService.showCustomDialogAndWait(typeChoiceDialog).orElse(null);
            if (selectedType == null) {
                return;
            }

            trackNewEntry(selectedType);
            tabSupplier.get().insertEntry(new BibEntry(selectedType));
        }
    }

    private void trackNewEntry(EntryType type) {
        Map<String, String> properties = new HashMap<>();
        properties.put("EntryType", type.getName());

        Telemetry.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, new HashMap<>()));
    }
}
