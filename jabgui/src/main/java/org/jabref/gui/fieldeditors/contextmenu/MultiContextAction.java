package org.jabref.gui.fieldeditors.contextmenu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.slf4j.LoggerFactory;

public class MultiContextAction extends SimpleCommand {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MultiContextAction.class);
    private final StandardActions command;
    private final ObservableList<LinkedFileViewModel> selectedFiles;
    private final BibDatabaseContext databaseContext;
    private final ObservableOptionalValue<BibEntry> bibEntry;
    private final GuiPreferences preferences;
    private final LinkedFilesEditorViewModel viewModel;

    public MultiContextAction(StandardActions command,
                              ObservableList<LinkedFileViewModel> selectedFiles,
                              BibDatabaseContext databaseContext,
                              ObservableOptionalValue<BibEntry> bibEntry,
                              GuiPreferences preferences,
                              LinkedFilesEditorViewModel viewModel) {
        this.command = command;
        this.selectedFiles = selectedFiles;
        this.databaseContext = databaseContext;
        this.bibEntry = bibEntry;
        this.preferences = preferences;
        this.viewModel = viewModel;

        this.executable.bind(Bindings.createBooleanBinding(
                () -> isExecutableFor(command),
                selectedFiles
        ));
    }

    @Override
    public void execute() {
        List<LinkedFileViewModel> files = List.copyOf(selectedFiles);

        switch (command) {
            case DOWNLOAD_FILE ->
                    files.stream()
                         .filter(this::isOnline)
                         .forEach(vm -> new ContextAction(
                                 StandardActions.DOWNLOAD_FILE, vm,
                                 databaseContext, bibEntry, preferences, viewModel
                         ).execute());

            case REDOWNLOAD_FILE ->
                    files.stream()
                         .filter(this::hasSourceUrl)
                         .forEach(vm -> new ContextAction(
                                 StandardActions.REDOWNLOAD_FILE, vm,
                                 databaseContext, bibEntry, preferences, viewModel
                         ).execute());

            case OPEN_FOLDER,
                 OPEN_FOLDERS -> {
                if (files.size() <= 1) {
                    for (LinkedFileViewModel vm : files) {
                        new ContextAction(command, vm, databaseContext, bibEntry, preferences, viewModel).execute();
                    }
                    return;
                }

                Map<Path, List<Path>> byDir = files.stream()
                                                   .map(vm -> vm.getFile().findIn(databaseContext, preferences.getFilePreferences()))
                                                   .flatMap(Optional::stream)
                                                   .collect(Collectors.groupingBy(p -> {
                                                       Path parent = p.getParent();
                                                       return parent != null ? parent : p;
                                                   }));

                for (Map.Entry<Path, List<Path>> e : byDir.entrySet()) {
                    Path fileToSelect = e.getValue().getFirst();
                    tryOpenFolderAndSelect(fileToSelect);
                }
            }

            default -> {
                for (LinkedFileViewModel vm : files) {
                    new ContextAction(command, vm, databaseContext, bibEntry, preferences, viewModel).execute();
                }
            }
        }
    }

    private boolean isExecutableFor(StandardActions action) {
        if (selectedFiles.isEmpty()) {
            return false;
        }

        return switch (action) {
            case OPEN_FILE,
                 OPEN_FILES,
                 OPEN_FOLDER,
                 OPEN_FOLDERS,
                 RENAME_FILE_TO_NAME,
                 DELETE_FILE,
                 DELETE_FILES ->
                    anySelected(this::isLocalAndExists);
            case DOWNLOAD_FILE,
                 DOWNLOAD_FILES ->
                    anySelected(this::isOnline);
            case REDOWNLOAD_FILE,
                 REDOWNLOAD_FILES ->
                    anySelected(this::hasSourceUrl);
            case MOVE_FILE_TO_FOLDER,
                 MOVE_FILES_TO_FOLDER,
                 MOVE_FILE_TO_FOLDER_AND_RENAME ->
                    allSelected(this::isMovableToDefaultDir);
            case RENAME_FILE_TO_PATTERN ->
                    false;
            default ->
                    true;
        };
    }

    private void tryOpenFolderAndSelect(Path fileToSelect) {
        try {
            NativeDesktop.get().openFolderAndSelectFile(fileToSelect);
        } catch (IOException e) {
            LOGGER.warn("Could not open folder ", e);
        }
    }

    private boolean isLocalAndExists(LinkedFileViewModel vm) {
        return !vm.getFile().isOnlineLink()
                && vm.getFile().findIn(databaseContext, preferences.getFilePreferences()).isPresent();
    }

    private boolean isOnline(LinkedFileViewModel vm) {
        return vm.getFile().isOnlineLink();
    }

    private boolean hasSourceUrl(LinkedFileViewModel vm) {
        return !vm.getFile().getSourceUrl().isEmpty();
    }

    private boolean isMovableToDefaultDir(LinkedFileViewModel vm) {
        return isLocalAndExists(vm) && !vm.isGeneratedPathSameAsOriginal();
    }

    private boolean allSelected(Predicate<LinkedFileViewModel> p) {
        for (LinkedFileViewModel vm : selectedFiles) {
            if (!p.test(vm)) {
                return false;
            }
        }

        return true;
    }

    private boolean anySelected(Predicate<LinkedFileViewModel> p) {
        for (LinkedFileViewModel vm : selectedFiles) {
            if (p.test(vm)) {
                return true;
            }
        }

        return false;
    }
}
