package org.jabref.gui.fieldeditors.contextmenu;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.copyfiles.CopySingleFileAction;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import java.util.ArrayList;
import java.util.List;

public class ContextMenuFactory {
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibDatabaseContext databaseContext;
    private final ObservableOptionalValue<BibEntry> bibEntry;
    private final LinkedFilesEditorViewModel viewModel;

    public ContextMenuFactory(DialogService dialogService,
                              GuiPreferences preferences,
                              BibDatabaseContext databaseContext,
                              ObservableOptionalValue<BibEntry> bibEntry,
                              LinkedFilesEditorViewModel viewModel) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.databaseContext = databaseContext;
        this.bibEntry = bibEntry;
        this.viewModel = viewModel;
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
                factory.createMenuItem(StandardActions.REMOVE_LINKS, new ContextMenuFactory.MultiContextAction(StandardActions.REMOVE_LINKS, selectedFiles, preferences))
        );

        return menu;
    }

    private ContextMenu createContextMenuForFile(LinkedFileViewModel linkedFile) {
        ContextMenu menu = new ContextMenu();
        ActionFactory factory = new ActionFactory();

        menu.getItems().addAll(
                factory.createMenuItem(StandardActions.EDIT_FILE_LINK, new ContextMenuFactory.ContextAction(StandardActions.EDIT_FILE_LINK, linkedFile, preferences)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.OPEN_FILE, new ContextMenuFactory.ContextAction(StandardActions.OPEN_FILE, linkedFile, preferences)),
                factory.createMenuItem(StandardActions.OPEN_FOLDER, new ContextMenuFactory.ContextAction(StandardActions.OPEN_FOLDER, linkedFile, preferences)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.DOWNLOAD_FILE, new ContextMenuFactory.ContextAction(StandardActions.DOWNLOAD_FILE, linkedFile, preferences)),
                factory.createMenuItem(StandardActions.RENAME_FILE_TO_PATTERN, new ContextMenuFactory.ContextAction(StandardActions.RENAME_FILE_TO_PATTERN, linkedFile, preferences)),
                factory.createMenuItem(StandardActions.RENAME_FILE_TO_NAME, new ContextMenuFactory.ContextAction(StandardActions.RENAME_FILE_TO_NAME, linkedFile, preferences)),
                factory.createMenuItem(StandardActions.MOVE_FILE_TO_FOLDER, new ContextMenuFactory.ContextAction(StandardActions.MOVE_FILE_TO_FOLDER, linkedFile, preferences)),
                factory.createMenuItem(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, new ContextMenuFactory.ContextAction(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, linkedFile, preferences)),
                factory.createMenuItem(StandardActions.COPY_FILE_TO_FOLDER, new CopySingleFileAction(linkedFile.getFile(), dialogService, databaseContext, preferences.getFilePreferences())),
                factory.createMenuItem(StandardActions.REDOWNLOAD_FILE, new ContextMenuFactory.ContextAction(StandardActions.REDOWNLOAD_FILE, linkedFile, preferences)),
                factory.createMenuItem(StandardActions.REMOVE_LINK, new ContextMenuFactory.ContextAction(StandardActions.REMOVE_LINK, linkedFile, preferences)),
                factory.createMenuItem(StandardActions.DELETE_FILE, new ContextMenuFactory.ContextAction(StandardActions.DELETE_FILE, linkedFile, preferences))
        );

        return menu;
    }


    private class MultiContextAction extends SimpleCommand {

        private final StandardActions command;
        private final ObservableList<LinkedFileViewModel> selectedFiles;
        private final CliPreferences preferences;

        public MultiContextAction(StandardActions command, ObservableList<LinkedFileViewModel> selectedFiles, CliPreferences preferences) {
            this.command = command;
            this.selectedFiles = selectedFiles;
            this.preferences = preferences;

            this.executable.bind(Bindings.createBooleanBinding(
                    () -> !selectedFiles.isEmpty(),
                    selectedFiles
            ));
        }

        @Override
        public void execute() {
                /*
                Must remove print statements before pull request.
                - use of standard IO causing failing tests in MainArchitectureTest.java
                 */
//            System.out.println("Executing MultiContextAction: " + command); /* MUST REMOVE BEFORE PULL REQUEST */
            List<LinkedFileViewModel> selectedFilesCopy = new ArrayList<>(selectedFiles);
            for (LinkedFileViewModel linkedFile : selectedFilesCopy) {
//                System.out.println("Processing file: " + linkedFile.getFile().getLink()); /* MUST REMOVE BEFORE PULL REQUEST */
                new ContextAction(command, linkedFile, preferences).execute();
//                System.out.println("Finished processing: " + linkedFile.getFile().getLink()); /* MUST REMOVE BEFORE PULL REQUEST */
            }

//            System.out.println("MultiContextAction completed"); /* MUST REMOVE BEFORE PULL REQUEST */
        }
    }

    private class ContextAction extends SimpleCommand {

        private final StandardActions command;
        private final LinkedFileViewModel linkedFile;

        public ContextAction(StandardActions command, LinkedFileViewModel linkedFile, CliPreferences preferences) {
            this.command = command;
            this.linkedFile = linkedFile;

            this.executable.bind(
                    switch (command) {
                        case RENAME_FILE_TO_PATTERN -> Bindings.createBooleanBinding(
                                () -> !linkedFile.getFile().isOnlineLink()
                                        && linkedFile.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent()
                                        && !linkedFile.isGeneratedNameSameAsOriginal(),
                                linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                        case MOVE_FILE_TO_FOLDER, MOVE_FILE_TO_FOLDER_AND_RENAME -> Bindings.createBooleanBinding(
                                () -> !linkedFile.getFile().isOnlineLink()
                                        && linkedFile.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent()
                                        && !linkedFile.isGeneratedPathSameAsOriginal(),
                                linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                        case DOWNLOAD_FILE -> Bindings.createBooleanBinding(
                                () -> linkedFile.getFile().isOnlineLink(),
                                linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                        case REDOWNLOAD_FILE -> Bindings.createBooleanBinding(
                                () -> !linkedFile.getFile().getSourceUrl().isEmpty(),
                                linkedFile.getFile().sourceUrlProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                        case OPEN_FILE, OPEN_FOLDER, RENAME_FILE_TO_NAME, DELETE_FILE -> Bindings.createBooleanBinding(
                                () -> !linkedFile.getFile().isOnlineLink()
                                        && linkedFile.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent(),
                                linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                        default -> BindingsHelper.constantOf(true);
                    });
        }

        @Override
        public void execute() {
            switch (command) {
                case EDIT_FILE_LINK -> linkedFile.edit();
                case OPEN_FILE -> linkedFile.open();
                case OPEN_FOLDER -> linkedFile.openFolder();
                case DOWNLOAD_FILE -> linkedFile.download(true);
                case REDOWNLOAD_FILE -> linkedFile.redownload();
                case RENAME_FILE_TO_PATTERN -> linkedFile.renameToSuggestion();
                case RENAME_FILE_TO_NAME -> linkedFile.askForNameAndRename();
                case MOVE_FILE_TO_FOLDER -> linkedFile.moveToDefaultDirectory();
                case MOVE_FILE_TO_FOLDER_AND_RENAME -> linkedFile.moveToDefaultDirectoryAndRename();
                case DELETE_FILE -> viewModel.deleteFile(linkedFile);
                case REMOVE_LINK, REMOVE_LINKS -> viewModel.removeFileLink(linkedFile);
            }
        }
    }
}
