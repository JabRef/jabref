package org.jabref.gui.fieldeditors.contextmenu;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.copyfiles.CopyLinkedFilesAction;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.jspecify.annotations.NonNull;

record SingleSelectionMenuBuilder(
        DialogService dialogService,
        BibDatabaseContext databaseContext,
        ObservableOptionalValue<BibEntry> bibEntry,
        GuiPreferences preferences,
        LinkedFilesEditorViewModel viewModel
) implements ContextMenuBuilder {

    SingleSelectionMenuBuilder(@NonNull DialogService dialogService,
                               @NonNull BibDatabaseContext databaseContext,
                               @NonNull ObservableOptionalValue<BibEntry> bibEntry,
                               @NonNull GuiPreferences preferences,
                               @NonNull LinkedFilesEditorViewModel viewModel) {
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.bibEntry = bibEntry;
        this.preferences = preferences;
        this.viewModel = viewModel;
    }

    @Override
    public boolean supports(ObservableList<LinkedFileViewModel> selection) {
        return selection != null && selection.size() == 1;
    }

    @Override
    public List<MenuItem> buildMenu(@NonNull ObservableList<LinkedFileViewModel> selection) {
        if (selection.isEmpty()) {
            return List.of();
        }

        LinkedFileViewModel selectedLinkedFile = selection.getFirst();
        ActionFactory factory = new ActionFactory();

        List<MenuItem> items = new ArrayList<>();

        items.add(factory.createMenuItem(
                StandardActions.EDIT_FILE_LINK,
                new ContextAction(StandardActions.EDIT_FILE_LINK, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));

        items.add(new SeparatorMenuItem());

        items.add(factory.createMenuItem(
                StandardActions.OPEN_FILE,
                new ContextAction(StandardActions.OPEN_FILE, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));
        items.add(factory.createMenuItem(
                StandardActions.OPEN_FOLDER,
                new ContextAction(StandardActions.OPEN_FOLDER, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));

        items.add(new SeparatorMenuItem());

        items.add(factory.createMenuItem(
                StandardActions.DOWNLOAD_FILE,
                new ContextAction(StandardActions.DOWNLOAD_FILE, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));

        items.add(factory.createMenuItem(
                StandardActions.RENAME_FILE_TO_PATTERN,
                new ContextAction(StandardActions.RENAME_FILE_TO_PATTERN, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));
        items.add(factory.createMenuItem(
                StandardActions.RENAME_FILE_TO_NAME,
                new ContextAction(StandardActions.RENAME_FILE_TO_NAME, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));

        items.add(factory.createMenuItem(
                StandardActions.MOVE_FILE_TO_FOLDER,
                new ContextAction(StandardActions.MOVE_FILE_TO_FOLDER, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));
        items.add(factory.createMenuItem(
                StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME,
                new ContextAction(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));

        items.add(factory.createMenuItem(
                StandardActions.COPY_FILE_TO_FOLDER,
                new CopyLinkedFilesAction(selectedLinkedFile.getFile(), dialogService, databaseContext, preferences.getFilePreferences())));

        items.add(factory.createMenuItem(
                StandardActions.REDOWNLOAD_FILE,
                new ContextAction(StandardActions.REDOWNLOAD_FILE, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));

        items.add(factory.createMenuItem(
                StandardActions.REMOVE_LINK,
                new ContextAction(StandardActions.REMOVE_LINK, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));

        items.add(factory.createMenuItem(
                StandardActions.DELETE_FILE,
                new ContextAction(StandardActions.DELETE_FILE, selectedLinkedFile, databaseContext, bibEntry, preferences, viewModel)));

        return items;
    }
}
