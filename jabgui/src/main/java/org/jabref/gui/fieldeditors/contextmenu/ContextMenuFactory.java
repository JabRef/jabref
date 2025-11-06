package org.jabref.gui.fieldeditors.contextmenu;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public @NonNull ContextMenu createMenuForSelection(ObservableList<LinkedFileViewModel> selection) {
        Objects.requireNonNull(selection, "selection must not be null");

        ContextMenu menu = new ContextMenu();
        if (selection.isEmpty()) {
            return menu;
        }

        Optional<ContextMenuBuilder> builder = menuBuilders.stream()
                                                           .filter(b -> b.supports(selection))
                                                           .findFirst();

        if (builder.isEmpty()) {
            return menu;
        }

        menu.getItems().setAll(builder.get().buildMenu(selection));
        return menu;
    }
}
