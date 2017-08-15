package org.jabref.gui.entryeditor.fileannotationtab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.ClipBoardManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.FileAnnotation;

public class FileAnnotationTabViewModel extends AbstractViewModel {

    private final ListProperty<FileAnnotationViewModel> annotations = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<String> files = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<FileAnnotationViewModel> currentAnnotation = new SimpleObjectProperty<>();

    private final FileAnnotationCache cache;
    private final BibEntry entry;
    private Map<String, List<FileAnnotation>> fileAnnotations;

    public FileAnnotationTabViewModel(FileAnnotationCache cache, BibEntry entry) {
        this.cache = cache;
        this.entry = entry;
        initialize();
    }

    private void initialize() {
        fileAnnotations = cache.getFromCache(entry);
        files.setAll(fileAnnotations.keySet());
    }

    public ObjectProperty<FileAnnotationViewModel> currentAnnotationProperty() {
        return currentAnnotation;
    }

    public ListProperty<FileAnnotationViewModel> annotationsProperty() {
        return annotations;
    }

    public ListProperty<String> filesProperty() {
        return files;
    }

    public void notifyNewSelectedAnnotation(FileAnnotationViewModel newAnnotation) {
        currentAnnotation.set(newAnnotation);
    }

    public void notifyNewSelectedFile(String newFile) {
        Comparator<FileAnnotation> byPage = Comparator.comparingInt(FileAnnotation::getPage);

        List<FileAnnotationViewModel> newAnnotations = fileAnnotations.getOrDefault(newFile, new ArrayList<>())
                .stream()
                .filter(annotation -> (null != annotation.getContent()))
                .sorted(byPage)
                .map(FileAnnotationViewModel::new)
                .collect(Collectors.toList());
        annotations.setAll(newAnnotations);
    }

    public void reloadAnnotations() {
        cache.remove(entry);
        initialize();
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

        new ClipBoardManager().setClipboardContents(sj.toString());
    }

    private FileAnnotationViewModel getCurrentAnnotation() {
        return currentAnnotation.get();
    }
}
