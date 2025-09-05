package org.jabref.gui.fieldeditors.contextmenu;

import java.util.List;
import java.util.Objects;

import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;

public class ContextMenuFactory {

    private final List<ContextMenuBuilder> menuBuilders;

    public ContextMenuFactory(DialogService dialogService,
                              GuiPreferences preferences,
                              BibDatabaseContext databaseContext,
                              ObservableOptionalValue<BibEntry> bibEntry,
                              LinkedFilesEditorViewModel viewModel) {

        Objects.requireNonNull(dialogService);
        Objects.requireNonNull(preferences);
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(bibEntry);
        Objects.requireNonNull(viewModel);

        this.menuBuilders = List.of(
                new SingleSelectionMenuBuilder(dialogService, databaseContext, bibEntry, preferences, viewModel),
                new MultiSelectionMenuBuilder(dialogService, databaseContext, bibEntry, preferences, viewModel)
        );
    }

    public ContextMenu createMenuForSelection(ObservableList<LinkedFileViewModel> selection) {
        if (selection == null || selection.isEmpty()) {
            return new ContextMenu();
        }

        ContextMenuBuilder builder = menuBuilders.stream()
                                                 .filter(s -> s.supports(selection))
                                                 .findFirst()
                                                 .orElseThrow(() -> new IllegalStateException(
                                                         "No ContextMenuBuilder found for selection (size=" + selection.size() + ")"));

        List<MenuItem> items = builder.buildMenu(selection);

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(items);
        return menu;
    }
}
