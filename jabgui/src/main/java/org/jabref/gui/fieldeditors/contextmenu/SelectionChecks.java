package org.jabref.gui.fieldeditors.contextmenu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.collections.ObservableList;

import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.LoggerFactory;

interface SelectionChecks {

    BibDatabaseContext databaseContext();

    GuiPreferences preferences();

    default boolean isLocalAndExists(LinkedFileViewModel vm) {
        return !vm.getFile().isOnlineLink()
                && vm.getFile().findIn(databaseContext(), preferences().getFilePreferences()).isPresent();
    }

    default boolean isOnline(LinkedFileViewModel vm) {
        return vm.getFile().isOnlineLink();
    }

    default boolean hasSourceUrl(LinkedFileViewModel vm) {
        return !vm.getFile().getSourceUrl().isEmpty();
    }

    default boolean isMovableToDefaultDir(LinkedFileViewModel vm) {
        return isLocalAndExists(vm) && !vm.isGeneratedPathSameAsOriginal();
    }

    default boolean allSelectedSatisfy(ObservableList<LinkedFileViewModel> sel,
                                       Predicate<LinkedFileViewModel> p) {
        return sel.stream().allMatch(p);
    }

    default boolean anySelectedSatisfy(ObservableList<LinkedFileViewModel> sel,
                                       Predicate<LinkedFileViewModel> p) {
        return sel.stream().anyMatch(p);
    }

    default void openContainingFolders(List<LinkedFileViewModel> vms) {
        Map<Path, List<Path>> byDir = vms.stream()
                                         .map(vm -> vm.getFile().findIn(databaseContext(), preferences().getFilePreferences()))
                                         .flatMap(Optional::stream)
                                         .collect(Collectors.groupingBy(p -> {
                                             Path parent = p.getParent();
                                             return parent != null ? parent : p;
                                         }));

        for (Map.Entry<Path, List<Path>> e : byDir.entrySet()) {
            Path fileToSelect = e.getValue().getFirst();
            try {
                NativeDesktop.get().openFolderAndSelectFile(fileToSelect);
            } catch (IOException ex) {
                LoggerFactory.getLogger(getClass()).warn("Could not open folder ", ex);
            }
        }
    }
}
