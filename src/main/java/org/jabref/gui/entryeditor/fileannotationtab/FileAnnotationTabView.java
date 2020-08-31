package org.jabref.gui.entryeditor.fileannotationtab;

import java.nio.file.Path;

import javax.inject.Inject;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;

import com.tobiasdiez.easybind.EasyBind;

public class FileAnnotationTabView {

    @FXML public ComboBox<Path> files;
    @FXML public ListView<FileAnnotationViewModel> annotationList;
    @FXML public Label author;
    @FXML public Label page;
    @FXML public Label date;
    @FXML public TextArea content;
    @FXML public TextArea marking;
    @FXML public VBox details;
    private final BibEntry entry;
    private final FileAnnotationCache fileAnnotationCache;
    private FileAnnotationTabViewModel viewModel;

    @Inject
    private FileUpdateMonitor fileMonitor;

    public FileAnnotationTabView(BibEntry entry, FileAnnotationCache fileAnnotationCache) {
        this.entry = entry;
        this.fileAnnotationCache = fileAnnotationCache;
    }

    @FXML
    public void initialize() {
        viewModel = new FileAnnotationTabViewModel(fileAnnotationCache, entry, fileMonitor);

        // Set-up files list
        files.getItems().setAll(viewModel.filesProperty().get());
        files.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> viewModel.notifyNewSelectedFile(newValue));
        files.getSelectionModel().selectFirst();

        // Set-up annotation list
        annotationList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        annotationList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> viewModel.notifyNewSelectedAnnotation(newValue));
        ViewModelListCellFactory<FileAnnotationViewModel> cellFactory = new ViewModelListCellFactory<FileAnnotationViewModel>()
                .withGraphic(this::createFileAnnotationNode);
        annotationList.setCellFactory(cellFactory);
        annotationList.setPlaceholder(new Label(Localization.lang("File has no attached annotations")));
        Bindings.bindContent(annotationList.itemsProperty().get(), viewModel.annotationsProperty());
        annotationList.getSelectionModel().selectFirst();
        annotationList.itemsProperty().get().addListener(
                (ListChangeListener<? super FileAnnotationViewModel>) c -> annotationList.getSelectionModel().selectFirst());

        // Set-up details pane
        content.textProperty().bind(EasyBind.select(viewModel.currentAnnotationProperty()).selectObject(FileAnnotationViewModel::contentProperty));
        marking.textProperty().bind(EasyBind.select(viewModel.currentAnnotationProperty()).selectObject(FileAnnotationViewModel::markingProperty));
        details.disableProperty().bind(viewModel.isAnnotationsEmpty());
    }

    private Node createFileAnnotationNode(FileAnnotationViewModel annotation) {
        GridPane node = new GridPane();

        ColumnConstraints firstColumn = new ColumnConstraints();
        ColumnConstraints secondColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(70);
        secondColumn.setPercentWidth(30);
        firstColumn.setHalignment(HPos.LEFT);
        secondColumn.setHalignment(HPos.RIGHT);
        node.getColumnConstraints().addAll(firstColumn, secondColumn);

        Label marking = new Label(annotation.getMarking());
        Label author = new Label(annotation.getAuthor());
        Label date = new Label(annotation.getDate());
        Label page = new Label(Localization.lang("Page") + ": " + annotation.getPage());

        marking.setStyle("-fx-font-size: 0.75em; -fx-font-weight: bold");
        marking.setMaxHeight(30);

        Tooltip markingTooltip = new Tooltip(annotation.getMarking());
        markingTooltip.setMaxWidth(800);
        markingTooltip.setWrapText(true);
        marking.setTooltip(markingTooltip);

        // add alignment for text in the list
        marking.setTextAlignment(TextAlignment.LEFT);
        marking.setAlignment(Pos.TOP_LEFT);
        marking.setMaxWidth(500);
        marking.setWrapText(true);
        author.setTextAlignment(TextAlignment.LEFT);
        author.setAlignment(Pos.TOP_LEFT);
        date.setTextAlignment(TextAlignment.RIGHT);
        date.setAlignment(Pos.TOP_RIGHT);
        page.setTextAlignment(TextAlignment.RIGHT);
        page.setAlignment(Pos.TOP_RIGHT);

        node.add(marking, 0, 0);
        node.add(author, 0, 1);
        node.add(date, 1, 0);
        node.add(page, 1, 1);

        return node;
    }

    public void copy() {
        viewModel.copyCurrentAnnotation();
    }
}
