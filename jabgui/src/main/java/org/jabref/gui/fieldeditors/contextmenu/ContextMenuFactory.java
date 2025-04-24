package org.jabref.gui.fieldeditors.contextmenu;

import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.copyfiles.CopySingleFileAction;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;

public class ContextMenuFactory {

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibDatabaseContext databaseContext;
    private final ObservableOptionalValue<BibEntry> bibEntry;
    private final LinkedFilesEditorViewModel viewModel;
    private final SingleContextCommandFactory singleCommandFactory;
    private final MultiContextCommandFactory multiCommandFactory;

    public ContextMenuFactory(DialogService dialogService,
                              GuiPreferences preferences,
                              BibDatabaseContext databaseContext,
                              ObservableOptionalValue<BibEntry> bibEntry,
                              LinkedFilesEditorViewModel viewModel,
                              SingleContextCommandFactory singleCommandFactory,
                              MultiContextCommandFactory multiCommandFactory) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.databaseContext = databaseContext;
        this.bibEntry = bibEntry;
        this.viewModel = viewModel;
        this.singleCommandFactory = singleCommandFactory;
        this.multiCommandFactory = multiCommandFactory;
    }

    public ContextMenu createForSelection(ObservableList<LinkedFileViewModel> selectedFiles) {
        if (selectedFiles.size() > 1) {
            return createContextMenuForMultiFile(selectedFiles);
        } else if (!selectedFiles.isEmpty()) {
            return createContextMenuForFile(selectedFiles.getFirst());
        }
        return new ContextMenu();
    }

    private ContextMenu createContextMenuForMultiFile(ObservableList<LinkedFileViewModel> selectedFiles) {
        ContextMenu menu = new ContextMenu();
        ActionFactory factory = new ActionFactory();

        menu.getItems().addAll(
                factory.createMenuItem(
                        StandardActions.REMOVE_LINKS,
                        multiCommandFactory.build(StandardActions.REMOVE_LINKS, selectedFiles)
                )
        );

        return menu;
    }

    private ContextMenu createContextMenuForFile(LinkedFileViewModel linkedFile) {
        ContextMenu menu = new ContextMenu();
        ActionFactory factory = new ActionFactory();

        menu.getItems().addAll(
                factory.createMenuItem(StandardActions.EDIT_FILE_LINK, singleCommandFactory.build(StandardActions.EDIT_FILE_LINK, linkedFile)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.OPEN_FILE, singleCommandFactory.build(StandardActions.OPEN_FILE, linkedFile)),
                factory.createMenuItem(StandardActions.OPEN_FOLDER, singleCommandFactory.build(StandardActions.OPEN_FOLDER, linkedFile)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.DOWNLOAD_FILE, singleCommandFactory.build(StandardActions.DOWNLOAD_FILE, linkedFile)),
                factory.createMenuItem(StandardActions.RENAME_FILE_TO_PATTERN, singleCommandFactory.build(StandardActions.RENAME_FILE_TO_PATTERN, linkedFile)),
                factory.createMenuItem(StandardActions.RENAME_FILE_TO_NAME, singleCommandFactory.build(StandardActions.RENAME_FILE_TO_NAME, linkedFile)),
                factory.createMenuItem(StandardActions.MOVE_FILE_TO_FOLDER, singleCommandFactory.build(StandardActions.MOVE_FILE_TO_FOLDER, linkedFile)),
                factory.createMenuItem(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, singleCommandFactory.build(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, linkedFile)),
                factory.createMenuItem(StandardActions.COPY_FILE_TO_FOLDER, new CopySingleFileAction(linkedFile.getFile(), dialogService, databaseContext, preferences.getFilePreferences())),
                factory.createMenuItem(StandardActions.REDOWNLOAD_FILE, singleCommandFactory.build(StandardActions.REDOWNLOAD_FILE, linkedFile)),
                factory.createMenuItem(StandardActions.REMOVE_LINK, singleCommandFactory.build(StandardActions.REMOVE_LINK, linkedFile)),
                factory.createMenuItem(StandardActions.DELETE_FILE, singleCommandFactory.build(StandardActions.DELETE_FILE, linkedFile))
        );

        return menu;
    }

    @FunctionalInterface
    public interface SingleContextCommandFactory {
        ContextAction build(StandardActions action, LinkedFileViewModel file);
    }

    @FunctionalInterface
    public interface MultiContextCommandFactory {
        MultiContextAction build(StandardActions action, ObservableList<LinkedFileViewModel> selectedFiles);
    }
}
