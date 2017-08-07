package org.jabref.gui.entryeditor;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.FieldName;
import org.jabref.model.pdf.FileAnnotation;

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

    public FxFileAnnotationTab(JabRefFrame frame, BasePanel basePanel, EntryEditor parent, FileAnnotationCache cache) {
        this.parent = parent;
        this.frame = frame;
        this.basePanel = basePanel;
        this.cache = cache;
        fileAnnotations = cache.getFromCache(parent.getEntry());

        this.panel = setupPanel(frame, basePanel);
        setText(Localization.lang("File annotations")); // TODO: rename in "File annotations"
        setTooltip(new Tooltip(Localization.lang("Show file annotations")));
        setGraphic(IconTheme.JabRefIcon.REQUIRED.getGraphicNode());
    }

    private Region setupPanel(JabRefFrame frame, BasePanel basePanel) {
        GridPane gridPane = new GridPane();
        gridPane.getStyleClass().add("editorPane");
        ColumnConstraints leftSideConstraint = new ColumnConstraints();
        leftSideConstraint.setPercentWidth(50);
        gridPane.getColumnConstraints().addAll(leftSideConstraint);

        gridPane.addColumn(0, setupLeftSide());
        gridPane.addColumn(1, setupRightSide());

        // Warp everything in a scroll-pane
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

        rightSide.addRow(0, new Label("Author"));


        rightSide.addRow(1, new Label("date"));

        rightSide.addRow(2, new Label("page"));
        rightSide.addRow(3, new Label("content"));

        rightSide.addRow(4, new Label("highlight"));
        return rightSide;
    }

    private GridPane setupLeftSide() {
        GridPane leftSide = new GridPane();

        leftSide.addColumn(0, new Label("Filename"));
        ComboBox<String> fileNameComboBox = createFileNameComboBox();
        GridPane.setHgrow(fileNameComboBox, Priority.ALWAYS);

        leftSide.addRow(0, fileNameComboBox);

        leftSide.add(createFileAnnotationsList(), 0, 1, 2, 1);
        updateShownAnnotations(fileAnnotations.get(fileNameComboBox.getSelectionModel().getSelectedItem()));

        return leftSide;
    }

    private ListView<FileAnnotation> createFileAnnotationsList() {
        ListView<FileAnnotation> listView = new ListView<>();
        listView.setItems(fileAnnotationsList);
        GridPane.setHgrow(listView, Priority.ALWAYS);

        listView.setCellFactory(new FileAnnotationListCellRenderer());
        return listView;
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
