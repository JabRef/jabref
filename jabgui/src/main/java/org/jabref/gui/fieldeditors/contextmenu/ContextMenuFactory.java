package org.jabref.gui.fieldeditors.contextmenu;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;

import org.jabref.gui.DialogService;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.jspecify.annotations.NonNull;

public class ContextMenuFactory {

    private final List<ContextMenuBuilder> menuBuilders;

    public ContextMenuFactory(@NonNull DialogService dialogService,
                              @NonNull GuiPreferences preferences,
                              @NonNull BibDatabaseContext databaseContext,
                              @NonNull ObservableOptionalValue<BibEntry> bibEntry,
                              @NonNull LinkedFilesEditorViewModel viewModel) {
        this.menuBuilders = List.of(
                new SingleSelectionMenuBuilder(dialogService, databaseContext, bibEntry, preferences, viewModel),
                new MultiSelectionMenuBuilder(dialogService, databaseContext, bibEntry, preferences, viewModel)
        );
    }

    public ContextMenu createMenuForSelection(@NonNull ObservableList<LinkedFileViewModel> selection) {
        if (selection.isEmpty()) {
            return new ContextMenu();
        }

        ContextMenuBuilder builder = menuBuilders.stream()
                                                 .filter(b -> b.supports(selection))
                                                 .findFirst()
                                                 .orElseThrow(() -> new IllegalStateException(
                                                         "No ContextMenuBuilder found for selection (size=" + selection.size() + ')'));

        ContextMenu menu = new ContextMenu();
        menu.getItems().setAll(builder.buildMenu(selection));
        return menu;
    }
}
