package org.jabref.gui.importer;

import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.entrytype.EntryTypeView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

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

    private final GuiPreferences preferences;

    public NewEntryAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, GuiPreferences preferences, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.preferences = preferences;

        this.type = Optional.empty();

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    public NewEntryAction(Supplier<LibraryTab> tabSupplier, EntryType type, DialogService dialogService, GuiPreferences preferences, StateManager stateManager) {
        this(tabSupplier, dialogService, preferences, stateManager);
        this.type = Optional.ofNullable(type);
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

            tabSupplier.get().insertEntry(new BibEntry(selectedType));
        }
    }
}
