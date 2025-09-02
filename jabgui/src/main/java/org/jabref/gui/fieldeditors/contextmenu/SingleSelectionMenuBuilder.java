package org.jabref.gui.fieldeditors.contextmenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;

record SingleSelectionMenuBuilder(
        BibDatabaseContext databaseContext,
        ObservableOptionalValue<BibEntry> bibEntry,
        GuiPreferences preferences,
        LinkedFilesEditorViewModel viewModel) implements ContextMenuBuilder, SelectionChecks {

    SingleSelectionMenuBuilder(BibDatabaseContext databaseContext,
                               ObservableOptionalValue<BibEntry> bibEntry,
                               GuiPreferences preferences,
                               LinkedFilesEditorViewModel viewModel) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.bibEntry = Objects.requireNonNull(bibEntry);
        this.preferences = Objects.requireNonNull(preferences);
        this.viewModel = Objects.requireNonNull(viewModel);
    }

    @Override
    public boolean supports(ObservableList<LinkedFileViewModel> selection) {
        return selection != null && selection.size() == 1;
    }

    @Override
    public List<MenuItem> buildMenu(ObservableList<LinkedFileViewModel> selection) {
        LinkedFileViewModel vm = selection.getFirst();
        List<MenuItem> items = new ArrayList<>();

        // Open file / folder — всегда доступны, если локальный файл существует
        if (isLocalAndExists(vm)) {
            items.add(actionItem(Localization.lang("Open file"),
                    new ContextAction(StandardActions.OPEN_FILE, vm, databaseContext, bibEntry, preferences, viewModel)));

            items.add(actionItem(Localization.lang("Open folder"),
                    new ContextAction(StandardActions.OPEN_FOLDER, vm, databaseContext, bibEntry, preferences, viewModel)));
        }

        // Download / Redownload — по условию
        if (isOnline(vm)) {
            items.add(actionItem(Localization.lang("Download file"),
                    new ContextAction(StandardActions.DOWNLOAD_FILE, vm, databaseContext, bibEntry, preferences, viewModel)));
        }
        if (hasSourceUrl(vm)) {
            items.add(actionItem(Localization.lang("Redownload file"),
                    new ContextAction(StandardActions.REDOWNLOAD_FILE, vm, databaseContext, bibEntry, preferences, viewModel)));
        }

        // Move / Rename / Remove / Delete — примеры базовых действий
        if (isMovableToDefaultDir(vm)) {
            items.add(actionItem(Localization.lang("Move file to file directory"),
                    new ContextAction(StandardActions.MOVE_FILE_TO_FOLDER, vm, databaseContext, bibEntry, preferences, viewModel)));
        }

        items.add(actionItem(Localization.lang("Rename file to name"),
                new ContextAction(StandardActions.RENAME_FILE_TO_NAME, vm, databaseContext, bibEntry, preferences, viewModel)));

        items.add(actionItem(Localization.lang("Remove link"),
                new ContextAction(StandardActions.REMOVE_LINK, vm, databaseContext, bibEntry, preferences, viewModel)));

        items.add(actionItem(Localization.lang("Permanently delete local file"),
                new ContextAction(StandardActions.DELETE_FILE, vm, databaseContext, bibEntry, preferences, viewModel)));

        return items;
    }

    private static MenuItem actionItem(String text, ContextAction action) {
        MenuItem mi = new MenuItem(text);
        mi.disableProperty().bind(action.executableProperty().not());
        mi.setOnAction(_ -> action.execute());
        return mi;
    }
}
