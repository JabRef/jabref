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

    private final DialogService dialogService;
    private final GuiPreferences preferences;

    private final List<ContextMenuBuilder> strategies;

    public ContextMenuFactory(DialogService dialogService,
                              GuiPreferences preferences,
                              BibDatabaseContext databaseContext,
                              ObservableOptionalValue<BibEntry> bibEntry,
                              LinkedFilesEditorViewModel viewModel) {

        this.dialogService = Objects.requireNonNull(dialogService);
        this.preferences = Objects.requireNonNull(preferences);
        BibDatabaseContext dbContext = Objects.requireNonNull(databaseContext);
        ObservableOptionalValue<BibEntry> currentEntry = Objects.requireNonNull(bibEntry);
        LinkedFilesEditorViewModel editorViewModel = Objects.requireNonNull(viewModel);

        this.strategies = List.of(
                new SingleSelectionMenuBuilder(dbContext, currentEntry, this.preferences, editorViewModel),
                new MultiSelectionMenuBuilder(dbContext, currentEntry, this.preferences, editorViewModel)
        );
    }

    public ContextMenu createForSelection(ObservableList<LinkedFileViewModel> selection) {
        if (selection == null || selection.isEmpty()) {
            return new ContextMenu();
        }

        ContextMenuBuilder builder = strategies.stream()
                                               .filter(s -> s.supports(selection))
                                               .findFirst()
                                               .orElseThrow(() -> new IllegalStateException(
                                                       "No menu strategy for selection size = " + selection.size()));

        List<MenuItem> items = builder.buildMenu(selection);

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(items);
        return menu;
    }
}
