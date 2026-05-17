package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.scene.control.Menu;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.FileDirectories;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class MoveFileSubmenuFactory {

    private final ActionFactory actionFactory;
    private final BibDatabaseContext databaseContext;
    private final GuiPreferences preferences;

    MoveFileSubmenuFactory(ActionFactory actionFactory,
                           BibDatabaseContext databaseContext,
                           GuiPreferences preferences) {
        this.actionFactory = actionFactory;
        this.databaseContext = databaseContext;
        this.preferences = preferences;
    }

    public Menu createSingle(LinkedFileViewModel linkedFileViewModel) {
        return createMenuItem(List.of(linkedFileViewModel), MoveFileCommand.Operation.SINGLE_MOVE);
    }

    public Menu createSingleAndRename(LinkedFileViewModel linkedFileViewModel) {
        return createMenuItem(List.of(linkedFileViewModel), MoveFileCommand.Operation.SINGLE_MOVE_AND_RENAME);
    }

    public Menu createForMulti(ObservableList<LinkedFileViewModel> selectedFiles) {
        return createMenuItem(selectedFiles, MoveFileCommand.Operation.MULTI_MOVE);
    }

    private Menu createMenuItem(List<LinkedFileViewModel> linkedFileViewModels, MoveFileCommand.Operation operation) {
        FileDirectories fileDirectories = databaseContext.getAllFileDirectories(preferences.getFilePreferences());
        Optional<Path> mainFileDirectory = preferences.getFilePreferences()
                                                      .getMainFileDirectory()
                                                      .map(path -> path.toAbsolutePath().normalize());

        Map<String, Optional<Path>> targetDirectories = Map.of(
                Localization.lang("Main file directory: %0"), mainFileDirectory,
                Localization.lang("Library-specific file directory: %0"), fileDirectories.getLibraryDirectory(),
                Localization.lang("User-specific file directory: %0"), fileDirectories.getUserDirectory(),
                Localization.lang("Next to library file: %0"), databaseContext.getDatabaseDirectory());

        Menu menu = actionFactory.createMenu(operation.getAction());
        for (Map.Entry<String, Optional<Path>> entry : targetDirectories.entrySet()) {
            Path targetDirectory = entry.getValue().orElse(null);
            String label = entry.getValue()
                                .map(path -> entry.getKey().formatted(path))
                                .orElse(entry.getKey().formatted(Localization.lang("Unavailable")));
            menu.getItems().add(actionFactory.createCustomMenuItem(
                    operation.getAction(),
                    new MoveFileCommand(targetDirectory, linkedFileViewModels, operation),
                    label
            ));
        }
        return menu;
    }

    private class MoveFileCommand extends SimpleCommand {

        private enum Operation {
            SINGLE_MOVE(StandardActions.MOVE_FILE_TO_FOLDER),
            SINGLE_MOVE_AND_RENAME(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME),
            MULTI_MOVE(StandardActions.MOVE_FILE_TO_FOLDER);

            private final StandardActions action;

            Operation(StandardActions action) {
                this.action = action;
            }

            private StandardActions getAction() {
                return action;
            }
        }

        private final @Nullable Path targetDirectory;
        private final List<LinkedFileViewModel> linkedFileViewModels;
        private final Operation operation;

        private MoveFileCommand(@Nullable Path targetDirectory,
                                List<LinkedFileViewModel> linkedFileViewModels,
                                Operation operation) {
            this.targetDirectory = targetDirectory;
            this.linkedFileViewModels = linkedFileViewModels;
            this.operation = operation;

            setExecutable(isMenuItemExecutable(targetDirectory, linkedFileViewModels, operation));
        }

        private boolean isMenuItemExecutable(@Nullable Path targetDirectory,
                                             List<LinkedFileViewModel> linkedFileViewModels,
                                             Operation operation) {
            if (targetDirectory == null) {
                return false;
            }

            return switch (operation) {
                case SINGLE_MOVE -> {
                    LinkedFileViewModel linkedFileViewModel = linkedFileViewModels.getFirst();
                    yield isMovable(linkedFileViewModel)
                            && !linkedFileViewModel.isInCurrentDirectory(targetDirectory);
                }
                case SINGLE_MOVE_AND_RENAME -> {
                    LinkedFileViewModel linkedFileViewModel = linkedFileViewModels.getFirst();
                    yield isMovable(linkedFileViewModel)
                            && (!linkedFileViewModel.isInCurrentDirectory(targetDirectory)
                            || !linkedFileViewModel.isGeneratedNameSameAsOriginal());
                }
                case MULTI_MOVE ->
                        linkedFileViewModels.stream().anyMatch(this::isMovable);
            };
        }

        @Override
        public void execute() {
            if (targetDirectory == null) {
                return;
            }

            switch (operation) {
                case SINGLE_MOVE ->
                        linkedFileViewModels.getFirst().moveToDirectory(targetDirectory);
                case SINGLE_MOVE_AND_RENAME ->
                        linkedFileViewModels.getFirst().moveToDirectoryAndRename(targetDirectory);
                case MULTI_MOVE ->
                        linkedFileViewModels.forEach(linkedFileViewModel -> {
                            if (isMovable(linkedFileViewModel) && !linkedFileViewModel.isInCurrentDirectory(targetDirectory)) {
                                linkedFileViewModel.moveToDirectory(targetDirectory);
                            }
                        });
            }
        }

        private boolean isMovable(LinkedFileViewModel linkedFileViewModel) {
            return !linkedFileViewModel.getFile().isOnlineLink()
                    && linkedFileViewModel.getFile()
                                          .findIn(databaseContext, preferences.getFilePreferences())
                                          .isPresent();
        }
    }
}
