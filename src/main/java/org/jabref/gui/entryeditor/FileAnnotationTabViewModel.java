package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.FileAnnotation;

public class FileAnnotationTabViewModel extends AbstractViewModel {

    private final ListProperty<FileAnnotationViewModel> annotations = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<String> files = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<FileAnnotationViewModel> currentAnnotation = new SimpleObjectProperty<>();
    private Map<String, List<FileAnnotation>> fileAnnotations;
    private StringProperty currentAuthor = new SimpleStringProperty();
    private StringProperty currentPage = new SimpleStringProperty();
    private StringProperty currentDate = new SimpleStringProperty();
    private StringProperty currentContent = new SimpleStringProperty();
    private StringProperty currentMarking = new SimpleStringProperty();
    public FileAnnotationTabViewModel(FileAnnotationCache cache, BibEntry entry) {
        fileAnnotations = cache.getFromCache(entry);
        files.addAll(fileAnnotations.keySet());
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
        /*
        currentAuthor.setValue(newValue.getAuthor());
        currentPage.setValue(newValue.getPage());
        currentDate.setValue(newValue.getTimeModified().toString());
        currentContent.setValue(getContentOrNA(newValue.getContent()));
        currentMarking.setValue(getMarking(newValue));
        */
    }

    private GridPane setupRightSide() {
        GridPane rightSide = new GridPane();


        rightSide.addRow(0, new Label("Author"));

        Text annotationAuthor = new Text();
        annotationAuthor.textProperty().bind(currentAuthor);
        rightSide.addColumn(1, annotationAuthor);

        rightSide.addRow(1, new Label("Page"));
        Text annotationPage = new Text();
        annotationPage.textProperty().bind(currentPage);

        rightSide.addColumn(1, annotationPage);

        rightSide.addRow(2, new Label("Date"));
        Text annotationDate = new Text();
        annotationDate.textProperty().bind(currentDate);

        rightSide.addColumn(1, annotationDate);

        rightSide.addRow(3, new Label("Content"));
        TextArea annotationContent = new TextArea();

        annotationContent.textProperty().bind(currentContent);
        annotationContent.setEditable(false);
        annotationContent.setWrapText(true);
        rightSide.addColumn(1, annotationContent);

        rightSide.addRow(4, new Label("Marking"));
        TextArea markingArea = new TextArea();
        markingArea.textProperty().bind(currentMarking);
        markingArea.setEditable(false);
        markingArea.setWrapText(true);
        rightSide.addColumn(1, markingArea);
        return rightSide;
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
}
