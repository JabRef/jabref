package org.jabref.gui.preferences.customimporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.importer.ImporterViewModel;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomImporterTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomImporterTabViewModel.class);

    private final ListProperty<ImporterViewModel> importers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ImporterViewModel> selectedImporters = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final PreferencesService preferences;
    private final DialogService dialogService;

    public CustomImporterTabViewModel(PreferencesService preferences, DialogService dialogService) {
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    @Override
    public void setValues() {
        Set<CustomImporter> importersLogic = preferences.getCustomImportFormats();
        for (CustomImporter importer : importersLogic) {
            importers.add(new ImporterViewModel(importer));
        }
    }

    @Override
    public void storeSettings() {
        preferences.storeCustomImportFormats(importers.stream()
                                                      .map(ImporterViewModel::getLogic)
                                                      .collect(Collectors.toSet()));
        Globals.IMPORT_FORMAT_READER.resetImportFormats(
                preferences.getImporterPreferences(),
                preferences.getGeneralPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getXmpPreferences(),
                Globals.getFileUpdateMonitor());
    }

    /**
     * Converts a path relative to a base-path into a class name.
     *
     * @param basePath base path
     * @param path     path that includes base-path as a prefix
     * @return class name
     */
    private static String pathToClass(String basePath, Path path) {
        String className = FileUtil.relativize(path, Collections.singletonList(Path.of(basePath))).toString();
        if (className != null) {
            int lastDot = className.lastIndexOf('.');
            if (lastDot < 0) {
                return className;
            }
            className = className.substring(0, lastDot);
        }
        return className;
    }

    public void addImporter() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CLASS, StandardFileType.JAR, StandardFileType.ZIP)
                .withDefaultExtension(StandardFileType.CLASS)
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();

        Optional<Path> selectedFile = dialogService.showFileOpenDialog(fileDialogConfiguration);

        if (selectedFile.isPresent() && (selectedFile.get().getParent() != null)) {
            boolean isArchive = FileUtil.getFileExtension(selectedFile.get())
                                        .filter(extension -> extension.equalsIgnoreCase("jar") || extension.equalsIgnoreCase("zip"))
                                        .isPresent();

            if (isArchive) {
                try {
                    Optional<Path> selectedFileInArchive = dialogService.showFileOpenFromArchiveDialog(selectedFile.get());
                    if (selectedFileInArchive.isPresent()) {
                        String className = selectedFileInArchive.get().toString().substring(0, selectedFileInArchive.get().toString().lastIndexOf('.')).replace(
                                "/", ".");
                        CustomImporter importer = new CustomImporter(selectedFile.get().toAbsolutePath().toString(), className);
                        importers.add(new ImporterViewModel(importer));
                    }
                } catch (IOException exc) {
                    LOGGER.error("Could not open ZIP-archive.", exc);
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("Could not open %0", selectedFile.get().toString()) + "\n"
                                    + Localization.lang("Have you chosen the correct package path?"),
                            exc);
                } catch (ClassNotFoundException exc) {
                    LOGGER.error("Could not instantiate importer", exc);
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("Could not instantiate %0 %1", "importer"),
                            exc);
                }
            } else {
                try {
                    String basePath = selectedFile.get().getParent().toString();
                    String className = pathToClass(basePath, selectedFile.get());
                    CustomImporter importer = new CustomImporter(basePath, className);

                    importers.add(new ImporterViewModel(importer));
                } catch (Exception exc) {
                    LOGGER.error("Could not instantiate importer", exc);
                    dialogService.showErrorDialogAndWait(Localization.lang("Could not instantiate %0", selectedFile.get().toString()), exc);
                } catch (NoClassDefFoundError exc) {
                    LOGGER.error("Could not find class while instantiating importer", exc);
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("Could not instantiate %0. Have you chosen the correct package path?", selectedFile.get().toString()),
                            exc);
                }
            }
        }
    }

    public void removeSelectedImporter() {
        importers.removeAll(selectedImporters);
    }

    public ListProperty<ImporterViewModel> selectedImportersProperty() {
        return selectedImporters;
    }

    public ListProperty<ImporterViewModel> importersProperty() {
        return importers;
    }
}
