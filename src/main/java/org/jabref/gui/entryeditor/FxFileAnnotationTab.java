package org.jabref.gui.entryeditor;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.FieldName;
import org.jabref.model.pdf.FileAnnotation;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jabref.model.pdf.FileAnnotationType.NONE;

public class FxFileAnnotationTab extends EntryEditorTab {
    private final Region panel;

    private final EntryEditor parent;
    private final JabRefFrame frame;
    private final BasePanel basePanel;
    private final FileAnnotationCache cache;
    private FieldEditorFX activeField;
    private Map<String, List<FileAnnotation>> fileAnnotations;
    private ObservableList<FileAnnotation> fileAnnotationsList = FXCollections.observableArrayList();

    private StringProperty currentAuthor = new SimpleStringProperty();
    private StringProperty currentPage = new SimpleStringProperty();
    private StringProperty currentDate = new SimpleStringProperty();
    private StringProperty currentContent = new SimpleStringProperty();
    private StringProperty currentMarking = new SimpleStringProperty();

    public FxFileAnnotationTab(JabRefFrame frame, BasePanel basePanel, EntryEditor parent, FileAnnotationCache cache) {
        this.parent = parent;
        this.frame = frame;
        this.basePanel = basePanel;
        this.cache = cache;
        fileAnnotations = cache.getFromCache(parent.getEntry());

        this.panel = setupPanel();
        setText(Localization.lang("File annotations"));
        setTooltip(new Tooltip(Localization.lang("Show file annotations")));
        setGraphic(IconTheme.JabRefIcon.REQUIRED.getGraphicNode());
    }

    private Region setupPanel() {
        GridPane gridPane = new GridPane();
        gridPane.getStyleClass().add("editorPane");
        ColumnConstraints leftSideConstraint = new ColumnConstraints();
        leftSideConstraint.setPercentWidth(50);
        gridPane.getColumnConstraints().addAll(leftSideConstraint);

        gridPane.addColumn(0, setupLeftSide());
        gridPane.addColumn(1, setupRightSide());

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }

    private GridPane setupRightSide() {
        GridPane rightSide = new GridPane();
        // TODO: add localization
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

        rightSide.addRow(5, new Button("Reload Annotations")); // Todo: add functionality
        rightSide.addColumn(1, new Button("Copy to Clipboard")); // Todo: add functionality
        return rightSide;
    }

    private String getMarking(FileAnnotation annotation) {
        if (annotation.hasLinkedAnnotation()) {
            return getContentOrNA(annotation.getLinkedFileAnnotation().getContent());
        }
        return "N/A";
    }

    private GridPane setupLeftSide() {
        GridPane leftSide = new GridPane();

        leftSide.addColumn(0, new Label("Filename"));
        ComboBox<String> fileNameComboBox = createFileNameComboBox();
        GridPane.setHgrow(fileNameComboBox, Priority.ALWAYS);

        leftSide.addRow(0, fileNameComboBox);

        ListView<FileAnnotation> listView = createFileAnnotationsList(fileNameComboBox);
        leftSide.add(listView, 0, 1, 2, 1);

        return leftSide;
    }

    private ListView<FileAnnotation> createFileAnnotationsList(ComboBox fileNameComboBox) {
        ListView<FileAnnotation> listView = new ListView<>();
        listView.setItems(fileAnnotationsList);
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listView.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<FileAnnotation>() {
                    @Override
                    public void changed(ObservableValue<? extends FileAnnotation> ov, FileAnnotation t, FileAnnotation newFA) {
                        currentAuthor.setValue(listView.getSelectionModel().getSelectedItem().getAuthor());
                        currentPage.setValue(Integer.toString(listView.getSelectionModel().getSelectedItem().getPage()));
                        currentDate.setValue(listView.getSelectionModel().getSelectedItem().getTimeModified().toString());
                        currentContent.setValue(getContentOrNA(listView.getSelectionModel().getSelectedItem().getContent()));
                        currentMarking.setValue(getMarking(listView.getSelectionModel().getSelectedItem()));
                    }
                }
        );
        GridPane.setHgrow(listView, Priority.ALWAYS);

        listView.setCellFactory(new FileAnnotationListCellRenderer());
        updateShownAnnotations(fileAnnotations.get(fileNameComboBox.getSelectionModel().getSelectedItem()));
        return listView;
    }

    private String getContentOrNA(String content) {
        if (content.isEmpty()) {
            return "N/A";
        }
        return content;
    }


    /**
     * Updates the list model to show the given notes without those with no content
     *
     * @param annotations value is the annotation name and the value is a pdfAnnotation object to add to the list model
     */
    private void updateShownAnnotations(List<FileAnnotation> annotations) {
        fileAnnotationsList.clear();
        if (annotations == null || annotations.isEmpty()) {
            fileAnnotationsList.add(new FileAnnotation("", LocalDateTime.now(), 0, Localization.lang("File has no attached annotations"), NONE, Optional.empty()));
        } else {
            Comparator<FileAnnotation> byPage = Comparator.comparingInt(FileAnnotation::getPage);
            annotations.stream()
                    .filter(annotation -> (null != annotation.getContent()))
                    .sorted(byPage)
                    .forEach(annotation -> fileAnnotationsList.add(new FileAnnotationViewModel(annotation)));
        }
    }

    private ComboBox<String> createFileNameComboBox() {

        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(fileAnnotations.keySet()));
        comboBox.getSelectionModel().selectFirst();
        return comboBox;
    }

    @Override
    public boolean shouldShow() {
        return parent.getEntry().getField(FieldName.FILE).isPresent();
    }

    @Override
    public void requestFocus() {
        if (activeField != null) {
            activeField.requestFocus();
        }
    }

    @Override
    protected void initialize() {
        setContent(panel);
    }
}
