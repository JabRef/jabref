package org.jabref.gui.fieldeditors.contextmenu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

interface SelectionChecks {

    Logger LOG = LoggerFactory.getLogger(SelectionChecks.class);

    BibDatabaseContext databaseContext();

    GuiPreferences preferences();

    default boolean isLocalAndExists(LinkedFileViewModel linkedFileViewModel) {
        return !linkedFileViewModel.getFile().isOnlineLink()
                && linkedFileViewModel.getFile().findIn(databaseContext(), preferences().getFilePreferences()).isPresent();
    }

    default boolean isOnline(LinkedFileViewModel linkedFileViewModel) {
        return linkedFileViewModel.getFile().isOnlineLink();
    }

    default boolean hasSourceUrl(LinkedFileViewModel linkedFileViewModel) {
        return !linkedFileViewModel.getFile().getSourceUrl().isEmpty();
    }

    default boolean isMovableToDefaultDir(LinkedFileViewModel linkedFileViewModel) {
        return isLocalAndExists(linkedFileViewModel) && !linkedFileViewModel.isGeneratedPathSameAsOriginal();
    }

    default void openContainingFolders(List<LinkedFileViewModel> linkedFileViewModels) {
        Map<Path, List<Path>> filesByDirectory = linkedFileViewModels.stream()
                .map(linkedFileViewModel -> linkedFileViewModel.getFile().findIn(databaseContext(), preferences().getFilePreferences()))
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(path -> {
                    Path parent = path.getParent();
                    return parent != null ? parent : path;
                }));

        for (Map.Entry<Path, List<Path>> entry : filesByDirectory.entrySet()) {
            Path fileToSelect = entry.getValue().getFirst();
            try {
                NativeDesktop.get().openFolderAndSelectFile(fileToSelect);
            } catch (IOException e) {
                LOG.warn("Could not open folder and select file: {}", fileToSelect, e);
            }
        }
    }
}
