package org.jabref.gui.importer;

import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.newentry.NewEntryApproach;
import org.jabref.gui.newentry.NewEntryView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryAction.class);

    private final Supplier<LibraryTab> tabSupplier;

    private final DialogService dialogService;

    private final GuiPreferences preferences;

    private NewEntryApproach initialApproach;
    private boolean isInstant;
    private Optional<EntryType> instantType;

    // Launches a dialog asking the user for inputs for the new entry to create.
    // This dialog initially opens to the last-used tab (from previous use of the tool).
    public NewEntryAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, GuiPreferences preferences, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.preferences = preferences;

        this.initialApproach = null;
        this.isInstant = false;
        this.instantType = Optional.empty();

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    // Launches a dialog asking the user for inputs for the new entry to create.
    // This dialog initially opens to the tab specified by `approach`. If `approach` is `null`, then the last-used tab
    // from previous use of the tool is restored.
    public NewEntryAction(NewEntryApproach approach, Supplier<LibraryTab> tabSupplier, DialogService dialogService, GuiPreferences preferences, StateManager stateManager) {
        this(tabSupplier, dialogService, preferences, stateManager);

        this.initialApproach = approach;
    }

    // Directly creates a new empty entry of the type `instantType`, without opening a dialog for the user to provide
    // inputs.
    // If `instantType` is `null`, the last-selected instant type from the previous use of the tool to create an empty
    // instance of a particular type is used (the `Article` standard entry type by default).
    public NewEntryAction(EntryType instantType, Supplier<LibraryTab> tabSupplier, DialogService dialogService, GuiPreferences preferences, StateManager stateManager) {
        this(tabSupplier, dialogService, preferences, stateManager);

        this.isInstant = true;
        this.instantType = Optional.ofNullable(instantType);
    }

    @Override
    public void execute() {
        // Without a tab supplier, we can only log an error message and abort.
        if (tabSupplier.get() == null) {
            // We skip logging the error if we were launched due to a keyboard shortcut though, since this isn't
            // something that can be disabled anyway.
            if (this.initialApproach == null && !this.isInstant) {
                LOGGER.error("Action 'New Entry' must be disabled when no database is open.");
            }
            return;
        }

        BibEntry newEntry;
        if (isInstant) {
            // If we're an instant action...
            final EntryType type;
            if (instantType.isPresent()) {
                // And we were created with an instant type, then we use that type.
                type = instantType.get();
            } else {
                // Otherwise, we query the last-selected entry type from the NewEntry dialogue.
                type = preferences.getNewEntryPreferences().getLatestInstantType();
            }
            // ...and create a new entry using this type.
            newEntry = new BibEntry(type);
        } else {
            // Otherwise, we launch a panel asking the user to specify details of the new entry.
            NewEntryView newEntryDialog = new NewEntryView(initialApproach, preferences, tabSupplier.get(), dialogService);
            newEntry = dialogService.showCustomDialogAndWait(newEntryDialog).orElse(null);
        }

        // This dialogue might handle inserting the new entry directly, so we don't do anything if the dialogue returns
        // `null`.
        if (newEntry != null) {
            tabSupplier.get().insertEntry(newEntry);
        }
    }
}
