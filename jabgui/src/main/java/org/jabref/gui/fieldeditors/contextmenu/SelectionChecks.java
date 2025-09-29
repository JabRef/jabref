package org.jabref.gui.fieldeditors.contextmenu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public record SelectionChecks(BibDatabaseContext databaseContext, GuiPreferences preferences) {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectionChecks.class);

    public SelectionChecks(BibDatabaseContext databaseContext, GuiPreferences preferences) {
        this.databaseContext = requireNonNull(databaseContext);
        this.preferences = requireNonNull(preferences);
    }

    public boolean isLocalAndExists(LinkedFileViewModel linkedFileViewModel) {
        return !linkedFileViewModel.getFile().isOnlineLink()
                && linkedFileViewModel.getFile().findIn(databaseContext(), preferences().getFilePreferences()).isPresent();
    }

    public boolean isOnline(LinkedFileViewModel linkedFileViewModel) {
        return linkedFileViewModel.getFile().isOnlineLink();
    }

    public boolean hasSourceUrl(LinkedFileViewModel linkedFileViewModel) {
        return !linkedFileViewModel.getFile().getSourceUrl().isEmpty();
    }

    public boolean isMovableToDefaultDir(LinkedFileViewModel linkedFileViewModel) {
        return isLocalAndExists(linkedFileViewModel)
                && !linkedFileViewModel.isGeneratedPathSameAsOriginal();
    }

    public void openContainingFolders(List<LinkedFileViewModel> linkedFileViewModels) {
        Map<Path, Path> representativeByDir = new LinkedHashMap<>();

        for (LinkedFileViewModel vm : linkedFileViewModels) {
            Optional<Path> resolved = vm.getFile().findIn(databaseContext(), preferences().getFilePreferences());
            if (resolved.isEmpty()) {
                continue;
            }

            Path file = resolved.get().toAbsolutePath().normalize();
            Path dir = (file.getParent() != null) ? file.getParent() : file;

            representativeByDir.putIfAbsent(dir, file);
        }

        for (Path fileToSelect : representativeByDir.values()) {
            try {
                NativeDesktop.get().openFolderAndSelectFile(fileToSelect);
            } catch (IOException e) {
                LOGGER.warn("Could not open folder and select file: {}", fileToSelect, e);
            }
        }
    }
}
