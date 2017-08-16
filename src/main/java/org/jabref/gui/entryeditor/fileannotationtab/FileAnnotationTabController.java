package org.jabref.gui.entryeditor.fileannotationtab;

import javax.inject.Inject;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.AbstractController;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.BibEntry;

import org.fxmisc.easybind.EasyBind;

public class FileAnnotationTabController extends AbstractController<FileAnnotationTabViewModel> {

    @FXML
    public ComboBox<String> files;
    @FXML
    public ListView<FileAnnotationViewModel> annotationList;
    @FXML
    public Label author;
    @FXML
    public Label page;
    @FXML
    public Label date;
    @FXML
    public TextArea content;
    @FXML
    public TextArea marking;

    @Inject
    private FileAnnotationCache fileAnnotationCache;
    @Inject
    private BibEntry entry;

    @FXML
    public void initialize() {
        viewModel = new FileAnnotationTabViewModel(fileAnnotationCache, entry);

        // Set-up files list
        files.getItems().setAll(viewModel.filesProperty().get());
        files.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> viewModel.notifyNewSelectedFile(newValue));
        files.getSelectionModel().selectFirst();

        // Set-up annotation list
        annotationList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        annotationList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> viewModel.notifyNewSelectedAnnotation(newValue));
        ViewModelListCellFactory<FileAnnotationViewModel> cellFactory = new ViewModelListCellFactory<FileAnnotationViewModel>()
                .withTooltip(FileAnnotationViewModel::getDescription)
                .withGraphic(annotation -> {
                    VBox node = new VBox();

                    Text text = new Text();
                    text.setText(annotation.getContent());
                    text.getStyleClass().setAll("text");

                    HBox details = new HBox();
                    details.getStyleClass().setAll("details");
                    Text page = new Text();
                    page.setText(Localization.lang("Page") + ": " + annotation.getPage());
                    details.getChildren().addAll(page);

                    node.getChildren().addAll(text, details);
                    node.setMaxWidth(Control.USE_PREF_SIZE);
                    return node;
                });
        annotationList.setCellFactory(cellFactory);
        annotationList.setPlaceholder(new Label(Localization.lang("File has no attached annotations")));
        Bindings.bindContent(annotationList.itemsProperty().get(), viewModel.annotationsProperty());
        annotationList.getSelectionModel().selectFirst();
        annotationList.itemsProperty().get().addListener(
                (ListChangeListener<? super FileAnnotationViewModel>) c -> annotationList.getSelectionModel().selectFirst());

        // Set-up details pane
        author.textProperty().bind(EasyBind.select(viewModel.currentAnnotationProperty()).selectObject(FileAnnotationViewModel::authorProperty));
        page.textProperty().bind(EasyBind.select(viewModel.currentAnnotationProperty()).selectObject(FileAnnotationViewModel::pageProperty));
        date.textProperty().bind(EasyBind.select(viewModel.currentAnnotationProperty()).selectObject(FileAnnotationViewModel::dateProperty));
        content.textProperty().bind(EasyBind.select(viewModel.currentAnnotationProperty()).selectObject(FileAnnotationViewModel::contentProperty));
        marking.textProperty().bind(EasyBind.select(viewModel.currentAnnotationProperty()).selectObject(FileAnnotationViewModel::markingProperty));
    }

    public void reloadAnnotations(ActionEvent event) {
        viewModel.reloadAnnotations();
    }

    public void copy(ActionEvent event) {
        viewModel.copyCurrentAnnotation();
    }
}
