package org.jabref.gui.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCustomizationDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCustomizationDialogViewModel.class);

    private final ListProperty<CustomImporter> importers;
    private final ListProperty<CustomImporter> selectedImporters = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final PreferencesService preferences;
    private final DialogService dialogService;

    public ImportCustomizationDialogViewModel(PreferencesService preferences, DialogService dialogService) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.importers = new SimpleListProperty<>(FXCollections.observableArrayList(Globals.prefs.customImports));
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
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY))
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
                        importers.add(importer);
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

                    importers.add(importer);
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

    public void saveToPrefs() {
        Globals.prefs.customImports.clear();
        Globals.prefs.customImports.addAll(importers);
        Globals.IMPORT_FORMAT_READER.resetImportFormats(
                Globals.prefs.getImportFormatPreferences(),
                Globals.prefs.getXmpPreferences(),
                Globals.getFileUpdateMonitor());
    }

    public ListProperty<CustomImporter> selectedImportersProperty() {
        return selectedImporters;
    }

    public ListProperty<CustomImporter> importersProperty() {
        return importers;
    }
}
