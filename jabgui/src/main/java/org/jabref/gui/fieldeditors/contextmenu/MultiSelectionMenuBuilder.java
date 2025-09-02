package org.jabref.gui.fieldeditors.contextmenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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

record MultiSelectionMenuBuilder(
        BibDatabaseContext databaseContext,
        ObservableOptionalValue<BibEntry> bibEntry,
        GuiPreferences preferences,
        LinkedFilesEditorViewModel viewModel
) implements ContextMenuBuilder, SelectionChecks {

    MultiSelectionMenuBuilder(BibDatabaseContext databaseContext,
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
        return selection != null && selection.size() > 1;
    }

    @Override
    public List<MenuItem> buildMenu(ObservableList<LinkedFileViewModel> selection) {
        List<MenuItem> items = new ArrayList<>();

        // Open file(s)
        items.add(batchActionItem(
                Localization.lang("Open file(s)"),
                selection,
                this::isLocalAndExists,
                StandardActions.OPEN_FILE));

        // Open folder(s) — группируем по директориям и выделяем файл
        items.add(customBatchItem(
                Localization.lang("Open folder(s)"),
                selection,
                this::isLocalAndExists,
                () -> openContainingFolders(selection)));

        // Download file(s)
        items.add(batchActionItem(
                Localization.lang("Download file(s)"),
                selection,
                this::isOnline,
                StandardActions.DOWNLOAD_FILE));

        // Redownload file(s)
        items.add(batchActionItem(
                Localization.lang("Redownload file(s)"),
                selection,
                this::hasSourceUrl,
                StandardActions.REDOWNLOAD_FILE));

        // Move file(s) to file directory
        items.add(batchActionItem(
                Localization.lang("Move file(s) to file directory"),
                selection,
                this::isMovableToDefaultDir,
                StandardActions.MOVE_FILE_TO_FOLDER));

        // Remove link(s) — доступно всегда при непустом выделении
        items.add(customBatchItem(
                Localization.lang("Remove link(s)"),
                selection,
                _ -> true,
                () -> selection.forEach(vm ->
                        new ContextAction(StandardActions.REMOVE_LINK, vm, databaseContext, bibEntry, preferences, viewModel).execute())));

        // Permanently delete local file(s)
        items.add(batchActionItem(
                Localization.lang("Permanently delete local file(s)"),
                selection,
                this::isLocalAndExists,
                StandardActions.DELETE_FILE));

        return items;
    }

    private MenuItem batchActionItem(String text,
                                     ObservableList<LinkedFileViewModel> selection,
                                     Predicate<LinkedFileViewModel> enablePredicate,
                                     StandardActions action) {
        MenuItem menuItem = new MenuItem(text);
        BooleanBinding enabled = anyMatches(selection, enablePredicate);
        menuItem.disableProperty().bind(enabled.not());

        menuItem.setOnAction(_ -> selection.stream()
                                           .filter(enablePredicate)
                                           .forEach(vm -> new ContextAction(action, vm, databaseContext, bibEntry, preferences, viewModel).execute()));

        return menuItem;
    }

    private MenuItem customBatchItem(String text,
                                     ObservableList<LinkedFileViewModel> selection,
                                     Predicate<LinkedFileViewModel> enablePredicate,
                                     Runnable action) {
        MenuItem menuItem = new MenuItem(text);
        BooleanBinding enabled = anyMatches(selection, enablePredicate);
        menuItem.disableProperty().bind(enabled.not());

        menuItem.setOnAction(_ -> action.run());
        return menuItem;
    }

    private static BooleanBinding anyMatches(ObservableList<LinkedFileViewModel> selection,
                                             Predicate<LinkedFileViewModel> predicate) {
        return Bindings.createBooleanBinding(
                () -> selection.stream().anyMatch(predicate),
                selection
        );
        // Если нужно "disabled, когда все НЕ подходят", просто .not() при биндинге выше.
    }
}
