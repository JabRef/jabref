package org.jabref.gui.fieldeditors.contextmenu;

import javafx.beans.binding.Bindings;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;

public class ContextAction extends SimpleCommand {

    private final StandardActions command;
    private final LinkedFileViewModel linkedFile;
    private final LinkedFilesEditorViewModel viewModel;

    public ContextAction(StandardActions command,
                         LinkedFileViewModel linkedFile,
                         BibDatabaseContext databaseContext,
                         ObservableOptionalValue<BibEntry> bibEntry,
                         GuiPreferences preferences,
                         LinkedFilesEditorViewModel viewModel) {
        this.command = command;
        this.linkedFile = linkedFile;
        this.viewModel = viewModel;

        this.executable.bind(
                switch (command) {
                    case RENAME_FILE_TO_PATTERN ->
                            Bindings.createBooleanBinding(
                                    () -> !linkedFile.getFile().isOnlineLink()
                                            && linkedFile.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent()
                                            && !linkedFile.isGeneratedNameSameAsOriginal(),
                                    linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                    case MOVE_FILE_TO_FOLDER,
                         MOVE_FILE_TO_FOLDER_AND_RENAME ->
                            Bindings.createBooleanBinding(
                                    () -> !linkedFile.getFile().isOnlineLink()
                                            && linkedFile.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent()
                                            && !linkedFile.isGeneratedPathSameAsOriginal(),
                                    linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                    case DOWNLOAD_FILE ->
                            Bindings.createBooleanBinding(
                                    () -> linkedFile.getFile().isOnlineLink(),
                                    linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                    case REDOWNLOAD_FILE ->
                            Bindings.createBooleanBinding(
                                    () -> !linkedFile.getFile().getSourceUrl().isEmpty(),
                                    linkedFile.getFile().sourceUrlProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                    case OPEN_FILE,
                         OPEN_FOLDER,
                         RENAME_FILE_TO_NAME,
                         DELETE_FILE ->
                            Bindings.createBooleanBinding(
                                    () -> !linkedFile.getFile().isOnlineLink()
                                            && linkedFile.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent(),
                                    linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                    default ->
                            BindingsHelper.constantOf(true);
                });
    }

    @Override
    public void execute() {
        switch (command) {
            case EDIT_FILE_LINK ->
                    linkedFile.edit();
            case OPEN_FILE ->
                    linkedFile.open();
            case OPEN_FOLDER ->
                    linkedFile.openFolder();
            case DOWNLOAD_FILE ->
                    linkedFile.download(true);
            case REDOWNLOAD_FILE ->
                    linkedFile.redownload();
            case RENAME_FILE_TO_PATTERN ->
                    linkedFile.renameToSuggestion();
            case RENAME_FILE_TO_NAME ->
                    linkedFile.askForNameAndRename();
            case MOVE_FILE_TO_FOLDER ->
                    linkedFile.moveToDefaultDirectory();
            case MOVE_FILE_TO_FOLDER_AND_RENAME ->
                    linkedFile.moveToDefaultDirectoryAndRename();
            case DELETE_FILE ->
                    viewModel.deleteFile(linkedFile);
            case REMOVE_LINK,
                 REMOVE_LINKS ->
                    viewModel.removeFileLink(linkedFile);
        }
    }
}
