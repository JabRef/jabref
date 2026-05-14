package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.FileDirectories;

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
        return createSingleMenu(StandardActions.MOVE_FILE_TO_FOLDER, linkedFileViewModel, false);
    }

    public Menu createSingleAndRename(LinkedFileViewModel linkedFileViewModel) {
        return createSingleMenu(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, linkedFileViewModel, true);
    }

    public Menu createForMulti(ObservableList<LinkedFileViewModel> selectedFiles) {
        FileDirectories fileDirectories = databaseContext.getAllFileDirectories(preferences.getFilePreferences());
        Optional<Path> mainFileDirectory = preferences.getFilePreferences()
                                                      .getMainFileDirectory()
                                                      .map(path -> path.toAbsolutePath().normalize());

        Menu menu = actionFactory.createMenu(StandardActions.MOVE_FILE_TO_FOLDER);
        menu.getItems().add(createItem(
                Localization.lang("Main file directory"),
                mainFileDirectory,
                selectedFiles
        ));
        menu.getItems().add(createItem(
                Localization.lang("Library-specific file directory"),
                fileDirectories.getLibraryDirectory(),
                selectedFiles
        ));
        menu.getItems().add(createItem(
                Localization.lang("User-specific file directory"),
                fileDirectories.getUserDirectory(),
                selectedFiles
        ));
        menu.getItems().add(createItem(
                Localization.lang("Next to library"),
                databaseContext.getDatabaseDirectory(),
                selectedFiles
        ));
        return menu;
    }

    private Menu createSingleMenu(StandardActions action,
                                  LinkedFileViewModel linkedFileViewModel,
                                  boolean renameAfterMove) {
        FileDirectories fileDirectories = databaseContext.getAllFileDirectories(preferences.getFilePreferences());
        Optional<Path> mainFileDirectory = preferences.getFilePreferences()
                                                      .getMainFileDirectory()
                                                      .map(path -> path.toAbsolutePath().normalize());

        Menu menu = actionFactory.createMenu(action);
        menu.getItems().add(createItem(
                Localization.lang("Main file directory"),
                mainFileDirectory,
                linkedFileViewModel,
                action,
                renameAfterMove
        ));
        menu.getItems().add(createItem(
                Localization.lang("Library-specific file directory"),
                fileDirectories.getLibraryDirectory(),
                linkedFileViewModel,
                action,
                renameAfterMove
        ));
        menu.getItems().add(createItem(
                Localization.lang("User-specific file directory"),
                fileDirectories.getUserDirectory(),
                linkedFileViewModel,
                action,
                renameAfterMove
        ));
        menu.getItems().add(createItem(
                Localization.lang("Next to library"),
                databaseContext.getDatabaseDirectory(),
                linkedFileViewModel,
                action,
                renameAfterMove
        ));
        return menu;
    }

    private MenuItem createItem(String directoryType,
                                Optional<Path> targetDirectory,
                                LinkedFileViewModel linkedFileViewModel,
                                StandardActions action,
                                boolean renameAfterMove) {
        String label = getMenuItemLabel(directoryType, targetDirectory);
        boolean movable = isMovable(linkedFileViewModel);
        boolean isMenuItemExecutable = movable
                && targetDirectory.isPresent()
                && (!linkedFileViewModel.isInCurrentDirectory(targetDirectory.get())
                || (renameAfterMove && !linkedFileViewModel.isGeneratedNameSameAsOriginal()));
        SimpleCommand command = new SimpleCommand() {
            @Override
            public void execute() {
                if (renameAfterMove) {
                    targetDirectory.ifPresent(linkedFileViewModel::moveToDirectoryAndRename);
                } else {
                    targetDirectory.ifPresent(linkedFileViewModel::moveToDirectory);
                }
            }
        };
        command.setExecutable(isMenuItemExecutable);
        return actionFactory.createCustomMenuItem(action, command, label);
    }

    private MenuItem createItem(String directoryType,
                                Optional<Path> targetDirectory,
                                ObservableList<LinkedFileViewModel> selectedFiles) {
        String label = getMenuItemLabel(directoryType, targetDirectory);
        boolean hasMovableFile = selectedFiles.stream().anyMatch(this::isMovable);
        boolean isMenuItemExecutable = hasMovableFile && targetDirectory.isPresent();
        SimpleCommand command = new SimpleCommand() {
            @Override
            public void execute() {
                if (targetDirectory.isEmpty()) {
                    return;
                }

                Path destinationDirectory = targetDirectory.get();
                selectedFiles.forEach(linkedFileViewModel -> {
                    if (isMovable(linkedFileViewModel) && !linkedFileViewModel.isInCurrentDirectory(destinationDirectory)) {
                        linkedFileViewModel.moveToDirectory(destinationDirectory);
                    }
                });
            }
        };
        command.setExecutable(isMenuItemExecutable);
        return actionFactory.createCustomMenuItem(StandardActions.MOVE_FILE_TO_FOLDER, command, label);
    }

    private String getMenuItemLabel(String directoryType, Optional<Path> directoryPath) {
        return directoryPath
                .map(path -> Localization.lang("%0: %1", directoryType, path.toAbsolutePath().normalize().toString()))
                .orElseGet(() -> Localization.lang("%0: %1", directoryType, Localization.lang("Unavailable")));
    }

    private boolean isMovable(LinkedFileViewModel linkedFileViewModel) {
        return !linkedFileViewModel.getFile().isOnlineLink()
                && linkedFileViewModel.getFile()
                                      .findIn(databaseContext, preferences.getFilePreferences())
                                      .isPresent();
    }
}
