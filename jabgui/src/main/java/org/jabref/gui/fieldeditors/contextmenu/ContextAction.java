package org.jabref.gui.fieldeditors.contextmenu;

import java.util.Arrays;
import java.util.Objects;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

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

        ObservableMap<Field, String> entryFieldsObservable = bibEntry.getValue()
                                                                     .map(BibEntry::getFieldsObservable)
                                                                     .orElse(FXCollections.emptyObservableMap());

        this.executable.bind(
                switch (command) {
                    case RENAME_FILE_TO_PATTERN ->
                            Bindings.createBooleanBinding(
                                    () -> !linkedFile.getFile().isOnlineLink()
                                            && linkedFile.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent()
                                            && !linkedFile.isGeneratedNameSameAsOriginal(),
                                    nonNullDependencies(
                                            linkedFile.getFile().linkProperty(),
                                            entryFieldsObservable
                                    )
                            );

                    case MOVE_FILE_TO_FOLDER,
                         MOVE_FILE_TO_FOLDER_AND_RENAME ->
                            Bindings.createBooleanBinding(
                                    () -> !linkedFile.getFile().isOnlineLink()
                                            && linkedFile.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent()
                                            && !linkedFile.isGeneratedPathSameAsOriginal(),
                                    nonNullDependencies(
                                            linkedFile.getFile().linkProperty(),
                                            entryFieldsObservable
                                    )
                            );

                    case DOWNLOAD_FILE ->
                            Bindings.createBooleanBinding(
                                    () -> linkedFile.getFile().isOnlineLink(),
                                    nonNullDependencies(
                                            linkedFile.getFile().linkProperty(),
                                            entryFieldsObservable
                                    )
                            );

                    case REDOWNLOAD_FILE ->
                            Bindings.createBooleanBinding(
                                    () -> !linkedFile.getFile().getSourceUrl().isEmpty(),
                                    nonNullDependencies(
                                            linkedFile.getFile().sourceUrlProperty(),
                                            entryFieldsObservable
                                    )
                            );

                    case OPEN_FILE,
                         OPEN_FOLDER,
                         RENAME_FILE_TO_NAME,
                         DELETE_FILE ->
                            Bindings.createBooleanBinding(
                                    () -> !linkedFile.getFile().isOnlineLink()
                                            && linkedFile.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent(),
                                    nonNullDependencies(
                                            linkedFile.getFile().linkProperty(),
                                            entryFieldsObservable
                                    )
                            );

                    default ->
                            BindingsHelper.constantOf(true);
                }
        );
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
            case DOWNLOAD_FILE -> {
                if (linkedFile.getFile().isOnlineLink()) {
                    linkedFile.download(true);
                }
            }
            case REDOWNLOAD_FILE -> {
                if (!linkedFile.getFile().getSourceUrl().isEmpty()) {
                    linkedFile.redownload();
                }
            }
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

    private static Observable[] nonNullDependencies(Observable... deps) {
        return Arrays.stream(deps)
                     .filter(Objects::nonNull)
                     .toArray(Observable[]::new);
    }
}
