package org.jabref.gui.entryeditor.fileannotationtab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.Globals;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileAnnotationTabViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileAnnotationTabViewModel.class);

    private final ListProperty<FileAnnotationViewModel> annotations = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Path> files = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<FileAnnotationViewModel> currentAnnotation = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanProperty annotationEmpty = annotations.emptyProperty();

    private final FileAnnotationCache cache;
    private final BibEntry entry;
    private Map<Path, List<FileAnnotation>> fileAnnotations;
    private Path currentFile;
    private final FileUpdateMonitor fileMonitor;
    private final FileUpdateListener fileListener = this::reloadAnnotations;

    public FileAnnotationTabViewModel(FileAnnotationCache cache, BibEntry entry, FileUpdateMonitor fileMonitor) {
        this.cache = cache;
        this.entry = entry;
        this.fileMonitor = fileMonitor;

        fileAnnotations = this.cache.getFromCache(this.entry);
        files.setAll(fileAnnotations.keySet());
    }

    public ObjectProperty<FileAnnotationViewModel> currentAnnotationProperty() {
        return currentAnnotation;
    }

    public ReadOnlyBooleanProperty isAnnotationsEmpty() {
        return annotationEmpty;
    }

    public ListProperty<FileAnnotationViewModel> annotationsProperty() {
        return annotations;
    }

    public ListProperty<Path> filesProperty() {
        return files;
    }

    public void notifyNewSelectedAnnotation(FileAnnotationViewModel newAnnotation) {
        currentAnnotation.set(newAnnotation);
    }

    public void notifyNewSelectedFile(Path newFile) {
        fileMonitor.removeListener(currentFile, fileListener);
        currentFile = newFile;

        Comparator<FileAnnotation> byPage = Comparator.comparingInt(FileAnnotation::getPage);

        List<FileAnnotationViewModel> newAnnotations = fileAnnotations
                .getOrDefault(currentFile, new ArrayList<>())
                .stream()
                .filter(annotation -> (null != annotation.getContent()))
                .sorted(byPage)
                .map(FileAnnotationViewModel::new)
                .collect(Collectors.toList());
        annotations.setAll(newAnnotations);

        try {
            fileMonitor.addListenerForFile(currentFile, fileListener);
        } catch (IOException e) {
            LOGGER.error("Problem while watching file for changes " + currentFile, e);
        }
    }

    private void reloadAnnotations() {
        // Make sure to always run this in the JavaFX thread as the file monitor (and its notifications) live in a different thread
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            // Remove annotations for the current entry and reinitialize annotation/cache
            cache.remove(entry);
            fileAnnotations = cache.getFromCache(entry);
            files.setAll(fileAnnotations.keySet());

            // Pretend that we just switched to the current file in order to refresh the display
            notifyNewSelectedFile(currentFile);
        });
    }

    /**
     * Copies the meta and content information of the pdf annotation to the clipboard
     */
    public void copyCurrentAnnotation() {
        if (null == getCurrentAnnotation()) {
            return;
        }
        StringJoiner sj = new StringJoiner("," + OS.NEWLINE);
        sj.add(Localization.lang("Author") + ": " + getCurrentAnnotation().getAuthor());
        sj.add(Localization.lang("Date") + ": " + getCurrentAnnotation().getDate());
        sj.add(Localization.lang("Page") + ": " + getCurrentAnnotation().getPage());
        sj.add(Localization.lang("Content") + ": " + getCurrentAnnotation().getContent());
        sj.add(Localization.lang("Marking") + ": " + getCurrentAnnotation().markingProperty().get());

        Globals.getClipboardManager().setContent(sj.toString());
    }

    private FileAnnotationViewModel getCurrentAnnotation() {
        return currentAnnotation.get();
    }
}
